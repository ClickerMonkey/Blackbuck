
package org.magnos.game.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.magnos.game.net.AbstractClient;
import org.magnos.game.net.Protocol;
import org.magnos.game.net.RemoteMethodCall;
import org.magnos.game.net.Server;


public class TcpClient extends AbstractClient
{

    public static final int HEADER_SIZE = 24;
    public static final int HEADER_PACKET_SIZE_OFFSET = 20;

    public SocketChannel socket;

    public TcpClient( Protocol protocol, InetSocketAddress address )
    {
        super( protocol, null, address );
    }

    public TcpClient( Protocol protocol, Server server, SocketChannel socket )
    {
        super( protocol, server, (InetSocketAddress)socket.socket().getRemoteSocketAddress() );

        this.socket = socket;
    }

    @Override
    protected void onInit() throws IOException
    {
        if (socket == null)
        {
            socket = protocol.getChannels().newSocket();
            socket.configureBlocking( false );
            socket.connect( address );
            
            while (!socket.finishConnect()) {
                // loop until connected
            }
        }
        else
        {
            socket.configureBlocking( false );
        }
    }

    @Override
    protected void onUpdate() throws IOException
    {
        write();
    }

    @Override
    protected void onClose() throws IOException
    {
        socket.close();
    }

    @Override
    protected void onWrite( ByteBuffer packet ) throws IOException
    {
        socket.write( packet );
    }

    @Override
    protected int onRead( ByteBuffer out ) throws IOException
    {
        return socket.read( out );
    }
    
    @Override
    protected void onCallWrite(RemoteMethodCall call)
    {
        
    }

    @Override
    protected int onReadPacketHeader( ByteBuffer in )
    {
        if (in.remaining() < HEADER_SIZE)
        {
            return ON_READ_REWIND;
        }

        int magicNumber = in.getInt();
        
        if (magicNumber != protocol.getMagicNumber())
        {
            return ON_READ_CLOSE;
        }
        
        long packetTime = in.getLong();
        long receivedTime = in.getLong();
        int packetSize = in.getInt();
        
        if (in.remaining() < packetSize)
        {
            return ON_READ_REWIND;
        }
        
        pingTime = System.nanoTime() - receivedTime;
        lastReceivedPacketSize = packetSize;
        lastReceivedPacketTime = packetTime;

        return packetSize;
    }
    
    @Override
    protected void onWritePacketHeader( ByteBuffer out )
    {
        out.putInt( protocol.getMagicNumber() );
        out.putLong( System.nanoTime() );
        out.putLong( lastReceivedPacketTime );
        out.putInt( 0 );
    }

    @Override
    protected int onWritePacketSize( ByteBuffer out )
    {
        int packetSize = out.limit() - HEADER_SIZE;

        out.putInt( HEADER_PACKET_SIZE_OFFSET, packetSize );

        return packetSize;
    }

}
