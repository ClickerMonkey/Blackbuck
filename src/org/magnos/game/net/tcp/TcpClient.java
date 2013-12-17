
package org.magnos.game.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.magnos.game.net.AbstractClient;
import org.magnos.game.net.PacketHeader;
import org.magnos.game.net.Protocol;
import org.magnos.game.net.Server;


public class TcpClient extends AbstractClient
{

    public static final int HEADER_SIZE = 16;
    public static final int HEADER_PACKET_SIZE_OFFSET = 24;

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
            socket = SocketChannel.open();
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
        if (socket.isOpen())
        {
            socket.close();
        }
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
    protected boolean onReadPacketHeader( ByteBuffer in, PacketHeader packet )
    {
        if (in.remaining() < HEADER_SIZE)
        {
            return false;
        }

        packet.magicNumber = in.getInt();
        packet.index = in.getInt();
        packet.time = in.getLong();
        packet.receivedTime = in.getLong();
        packet.size = in.getInt();

        if (in.remaining() < packet.size)
        {
            return false;
        }

        return true;
    }

    @Override
    protected void onWritePacketHeader( ByteBuffer out )
    {
        out.putInt( protocol.getMagicNumber() );
        out.putInt( packetIndex );
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
