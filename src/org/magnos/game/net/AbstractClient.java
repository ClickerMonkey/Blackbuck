package org.magnos.game.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import org.magnos.game.net.util.IterableSingle;
import org.magnos.reflect.util.Compress;

public abstract class AbstractClient implements Client
{

	protected final Protocol protocol;
	protected final Server server;
	protected final String host;
	protected final int port;
	
	protected Queue<RemoteMethodCall> outbound;
	protected Queue<RemoteMethodCall> inbound;
	
	protected ByteBuffer buffer;
	protected Queue<ByteBuffer> bufferOut;
	
	protected boolean initialized;
	protected long updateRate;
	protected long lastUpdateTime;
	protected boolean readyToSend;
	protected boolean closed;
	protected int states;
	
	protected long nextTime;
	
	protected long durationRead;
	protected long durationUpdate;
	protected long durationWrite;
	protected long durationSend;
	
	protected int packetIndex;
	protected int callsSent;
	protected long lastReceivedPacketTime;
	protected int lastReceivedPacketSize;
	protected int lastSentPacketSize;
	protected int packetsRead;
	protected long pingTime;
	
	public AbstractClient(Protocol protocol, Server server, String host, int port)
	{
		this.protocol = protocol;
		this.server = server;
		this.host = host;
		this.port = port;
		this.initialized = false;
		this.nextTime = System.currentTimeMillis();
	}

	protected abstract void onInit() throws IOException;
	protected abstract void onUpdate() throws IOException;
	protected abstract void onClose() throws IOException;
	protected abstract void onWrite(ByteBuffer packet) throws IOException;
	protected abstract int onRead(ByteBuffer out) throws IOException;
	
	private boolean isNotReady()
	{
		return closed || !initialized;
	}
	
	@Override
	public void init() throws IOException
	{
		try
		{
			onInit();
			
			initialized = true;
		}
		catch (IOException e)
		{
			throw e;
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		
		if (initialized)
		{
			outbound = new ArrayDeque<RemoteMethodCall>();
			inbound = new ArrayDeque<RemoteMethodCall>();
			bufferOut = new ArrayDeque<ByteBuffer>();
			buffer = protocol.allocateBuffer();
		}
	}
	
	@Override
	public boolean isInitialized()
	{
		return initialized;
	}

	@Override
	public void read() throws IOException
	{
		if (isNotReady()) {
			return;
		}
		
		long startTime = System.nanoTime();
		
		inbound.clear();
		
		packetsRead = 0;
		
		int read = onRead( buffer );
		
		if (read == -1)
		{
			close();
		}
		else
		{
			buffer.flip();
			
			while (readBuffer())
			{
				packetsRead++;
			}
		}
		
		durationRead = System.nanoTime() - startTime;
	}
	
	private boolean readBuffer()
	{
		if (buffer.remaining() < Protocol.HEADER_SIZE)
		{
			unflip( buffer );
			
			return false;
		}
		
		int magicNumber = buffer.getInt();
		
		if (magicNumber != protocol.getMagicNumber())
		{
			close();
			
			return false;
		}
		
		int packetIndex = buffer.getInt();
		long packetTime = buffer.getLong();
		long receivedTime = buffer.getLong();
		int packetSize = buffer.getInt();
		
		if (buffer.remaining() < packetSize)
		{
			unflip( buffer );
			
			return false;
		}
		
		pingTime = System.nanoTime() - receivedTime;
		
		int callCount = 0;
		int finalPosition = buffer.position() + packetSize;
		
		while (buffer.position() < finalPosition)
		{
			int interfaceId = Compress.getIntUnsigned( buffer );
			int methodId = Compress.getIntUnsigned( buffer );
			
			try
			{
				RemoteMethodEntry entry = protocol.getEntry( interfaceId, methodId );
				
				Object[] arguments = entry.reflectMethod.get( buffer );
				
				entry.method.invoke( entry.listener, arguments );
				callCount++;
			}
			catch (Exception e)
			{
				throw new RuntimeException( e );
			}
		}
		
		lastReceivedPacketSize = packetSize;
		lastReceivedPacketTime = packetTime;
	}
	
	private void unflip( ByteBuffer bb )
	{
		int readMax = bb.limit();
		bb.limit( bb.capacity() );
		bb.position( readMax );
	}

	@Override
	public void update() throws IOException
	{
		if (isNotReady()) {
			return;
		}

		long startTime = System.nanoTime();
		long currentTime = System.currentTimeMillis();
		
		readyToSend = (currentTime >= nextTime);
		
		if (readyToSend)
		{
			while (nextTime <= currentTime)
			{
				nextTime += updateRate;
			}
		}
		
		onUpdate();
		
		durationUpdate = System.nanoTime() - startTime;
	}

	@Override
	public boolean isReadyToSend()
	{
		return readyToSend;
	}

	@Override
	public void queue( RemoteMethodCall call )
	{
		outbound.add( call );
	}

	@Override
	public void send() throws IOException
	{
		if (isNotReady()) {
			return;
		}
		
		long startTime = System.nanoTime();
		
		ByteBuffer out = protocol.allocateBuffer();
		
		out.clear();
		out.putInt( protocol.getMagicNumber() );
		out.putInt( packetIndex );
		out.putLong( System.nanoTime() );
		out.putLong( lastReceivedPacketTime );
		out.putInt( 0 );
		
		callsSent = 0;
		
		while (!outbound.isEmpty() && tryPut(outbound.peek(), out))
		{
			outbound.peek();
			callsSent++;
		}

		out.flip();
		
		if (callsSent > 0)
		{
			lastSentPacketSize = out.limit() - Protocol.HEADER_SIZE;
			
			out.putInt( Protocol.HEADER_PACKET_SIZE_OFFSET, lastSentPacketSize );
			
			bufferOut.offer( out );
			
			packetIndex++;
			
			write();
		}
		else
		{
			protocol.releaseBuffer( out );
		}
		
		durationSend = System.nanoTime() - startTime;
	}
	
	private boolean tryPut( RemoteMethodCall call, ByteBuffer dest )
	{
		int interfaceId = call.remoteInterface.id();
		int interfaceIdSize = Compress.sizeOfUnsigned( interfaceId );
		int methodId = call.remoteMethod.id();
		int methodIdSize = Compress.sizeOfUnsigned( methodId );
		int requiredSize = call.callSize + interfaceIdSize + methodIdSize;
		boolean available = (dest.remaining() >= requiredSize); 
		
		if (available)
		{
			Compress.putIntUnsigned( dest, interfaceId );
			Compress.putIntUnsigned( dest, methodId );
			call.reflectMethod.put( dest, call.arguments );
		}
		
		return available;
	}

	@Override
	public void sendAllNow() throws IOException
	{
		if (isNotReady()) {
			return;
		}
		
		while (!outbound.isEmpty())
		{
			send();
		}
		
		while (!bufferOut.isEmpty())
		{
			write();
		}
	}

	private void write() throws IOException
	{
		if (isNotReady()) {
			return;
		}
		
		long startTime = System.nanoTime();
		
		while (!bufferOut.isEmpty())
		{
			ByteBuffer next = bufferOut.peek();
			
			onWrite( next );
			
			if (next.hasRemaining())
			{
				break;
			}
			
			bufferOut.poll();
			protocol.releaseBuffer( next );
		}
		
		durationRead = System.nanoTime() - startTime;
	}
	
	@Override
	public void close()
	{
		if (isNotReady()) {
			return;
		}
		
		try {
			onClose();
		} catch (IOException e) {
			// ignore
		} finally {
			closed = true;

			if (initialized) {
				protocol.releaseBuffer( buffer );
				for (ByteBuffer bb : bufferOut) {
					protocol.releaseBuffer( bb );
				}
				bufferOut = null;
				buffer = null;
				inbound = null;
				outbound = null;
				initialized = false;
			}
		}
	}

	@Override
	public boolean isClosed()
	{
		return closed;
	}

	@Override
	public Server getServer()
	{
		return server;
	}

	@Override
	public Protocol getProtocol()
	{
		return protocol;
	}

	@Override
	public <T> T newService( Class<T> remoteInterface )
	{
		Service<T> service = new Service<T>( remoteInterface );
		
		service.setTarget( new IterableSingle<Client>( this ) );
		
		return service.get();
	}

	@Override
	public String getHost()
	{
		return host;
	}

	@Override
	public int getPort()
	{
		return port;
	}

	@Override
	public Queue<RemoteMethodCall> getOutbound()
	{
		return outbound;
	}

	@Override
	public Queue<RemoteMethodCall> getInbound()
	{
		return inbound;
	}

	@Override
	public long getUpdateRate()
	{
		return updateRate;
	}

	@Override
	public void setUpdateRate( long millis )
	{
		updateRate = millis;
	}

	@Override
	public long getLastUpdateTime()
	{
		return nextTime - updateRate;
	}

	@Override
	public int getCallsSent()
	{
		return 0;
	}

	@Override
	public int getPacketsRead()
	{
		return 0;
	}

	@Override
	public int getPacketIndex()
	{
		return 0;
	}

	@Override
	public int getLastPacketSize()
	{
		return 0;
	}

	@Override
	public long getReadNanos()
	{
		return durationRead;
	}

	@Override
	public long getUpdateNanos()
	{
		return durationUpdate;
	}

	@Override
	public long getWriteNanos()
	{
		return durationWrite;
	}

	@Override
	public long getSendNanos()
	{
		return durationSend;
	}

	@Override
	public int getStates()
	{
		return states;
	}

	@Override
	public void setStates( int x )
	{
		states = x;
	}

	@Override
	public void addState( int x )
	{
		states |= x;
	}

	@Override
	public void removeState( int x )
	{
		states &= ~x;
	}

}
