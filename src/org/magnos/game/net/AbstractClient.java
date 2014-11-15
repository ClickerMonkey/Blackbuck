package org.magnos.game.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

import org.magnos.game.net.util.IterableSingle;
import org.magnos.reflect.util.Compress;

public abstract class AbstractClient implements Client
{
    protected static final int ON_READ_REWIND = -1;
    protected static final int ON_READ_CLOSE = -2;
    
	protected final Protocol protocol;
	protected final Server server;
	protected final InetSocketAddress address;
	
	protected PriorityQueue<RemoteMethodCall> outbound;
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
	protected int sendIndex;
	
	protected Object attachment;
	
	public AbstractClient(Protocol protocol, Server server, InetSocketAddress address)
	{
		this.protocol = protocol;
		this.server = server;
		this.address = address;
		this.initialized = false;
		this.closed = true;
		this.nextTime = System.currentTimeMillis();
	}

	protected abstract void onInit() throws IOException;
	protected abstract void onUpdate() throws IOException;
	protected abstract void onClose() throws IOException;
	protected abstract void onWrite(ByteBuffer packet) throws IOException;
	protected abstract int onRead(ByteBuffer out) throws IOException;
	protected abstract void onCallWrite(RemoteMethodCall call);

	protected abstract int onReadPacketHeader(ByteBuffer in);
	protected abstract void onWritePacketHeader(ByteBuffer out);
	protected abstract int onWritePacketSize(ByteBuffer out );
	
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
			outbound = new PriorityQueue<RemoteMethodCall>();
			inbound = new ArrayDeque<RemoteMethodCall>();
			bufferOut = new ArrayDeque<ByteBuffer>();
			buffer = protocol.allocateBuffer();
			lastUpdateTime = System.currentTimeMillis();
            closed = false;
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
		if (isNotReady()) 
		{
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
			
			while (!closed && readBuffer(buffer))
			{
				packetsRead++;
			}
			
			if (!closed && packetsRead > 0)
			{
			    compact( buffer );
			}
		}
		
		durationRead = System.nanoTime() - startTime;
	}
	
	protected boolean readBuffer(ByteBuffer buffer)
	{
	    int result = onReadPacketHeader( buffer ); 
	    
	    if (result == ON_READ_REWIND)
	    {
	        unflip( buffer );
	        
	        return false;
	    }
	    else if (result == ON_READ_CLOSE)
	    {
	        close();
            
            return false;
	    }
	    
		int finalPosition = buffer.position() + result;
		
		while (!closed && buffer.position() < finalPosition)
		{
			int interfaceId = Compress.getIntUnsigned( buffer );
			int methodId = Compress.getIntUnsigned( buffer );
			
			try
			{
				RemoteMethodEntry entry = protocol.getEntry( interfaceId, methodId );
				Object[] arguments = entry.reflectMethod.get( buffer );
				
			    handleInvocation( entry, arguments );    
			}
			catch (Exception e)
			{
				throw new RuntimeException( e );
			}
		}
		
		return true;
	}
	
	private void handleInvocation( RemoteMethodEntry entry, Object[] arguments ) throws Exception
	{
	    RemoteMethod remoteMethod = entry.remoteMethod;
        Match readMatch = remoteMethod.readMatch();
        int readStates = remoteMethod.readStates();

        if (readMatch.isMatch( readStates, getStates() ) || 
           !protocol.notifyReadBlock(this, readMatch, readStates, getStates()))
        {
            if (entry.listener instanceof HasClient)
            {
                ((HasClient)entry.listener).setCurrentClient( this );
            }
            
            entry.method.invoke( entry.listener, arguments );
        }
	}
	
	private void unflip( ByteBuffer bb )
	{
		int readMax = bb.limit();
		bb.limit( bb.capacity() );
		bb.position( readMax );
	}
	
	private void compact( ByteBuffer bb )
	{
	    int newLimit = buffer.limit() - buffer.position();
        buffer.compact();
        buffer.position( 0 );
        buffer.limit( newLimit );
	}

	@Override
	public void update() throws IOException
	{
		if (isNotReady()) 
		{
		    readyToSend = false;
		    
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
		onWritePacketHeader( out );
		
		callsSent = 0;
		
		while (!outbound.isEmpty() && tryPut(outbound.peek(), out))
		{
			RemoteMethodCall call = outbound.poll();
			call.packetIndex = packetIndex;
			onCallWrite( call );
			callsSent++;
		}

		out.flip();
		
		if (callsSent > 0)
		{
			lastSentPacketSize = onWritePacketSize( out );
			
			bufferOut.offer( out );
			
			packetIndex++;
			
			write();
		}
		else
		{
			protocol.releaseBuffer( out );
		}
		
		durationSend = System.nanoTime() - startTime;
		sendIndex++;
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

	protected void write() throws IOException
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
		
		durationWrite = System.nanoTime() - startTime;
	}
	
	@Override
	public void close()
	{
		if (isNotReady()) 
		{
			return;
		}
		
		try 
		{
			onClose();
		} 
		catch (IOException e) 
		{
			// ignore
		}
		finally 
		{
			closed = true;

			if (initialized) 
			{
				protocol.releaseBuffer( buffer );
				
				for (ByteBuffer bb : bufferOut) 
				{
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
	public InetSocketAddress getAddress()
	{
	    return address;
	}

	@Override
	public PriorityQueue<RemoteMethodCall> getOutbound()
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
		return callsSent;
	}

	@Override
	public int getPacketsRead()
	{
		return packetsRead;
	}

	@Override
	public int getPacketIndex()
	{
		return packetIndex;
	}

	@Override
	public int getLastPacketSize()
	{
		return lastSentPacketSize;
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
	public void attach(Object attachment)
	{
		this.attachment = attachment;
	}
	
    @SuppressWarnings ("unchecked" )
    @Override
	public <T> T attachment()
	{
		return (T) attachment;
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
	
	@Override
	public int getSendIndex()
	{
		return sendIndex;
	}

}
