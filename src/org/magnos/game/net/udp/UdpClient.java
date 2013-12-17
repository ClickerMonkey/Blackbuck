package org.magnos.game.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.magnos.game.net.AbstractClient;
import org.magnos.game.net.PacketHeader;
import org.magnos.game.net.Protocol;
import org.magnos.game.net.Server;


public class UdpClient extends AbstractClient
{

    public ByteBuffer read;
    
    public UdpClient( Protocol protocol, Server server, InetSocketAddress address )
    {
        super( protocol, server, address );
    }

    @Override
    protected void onInit() throws IOException
    {
        read = protocol.allocateBuffer();
    }

    @Override
    protected void onUpdate() throws IOException
    {

    }

    @Override
    protected void onClose() throws IOException
    {
        protocol.releaseBuffer( read );
    }

    @Override
    protected void onWrite( ByteBuffer packet ) throws IOException
    {

    }

    @Override
    protected int onRead( ByteBuffer out ) throws IOException
    {
        int bytes = read.remaining();
        
        out.put( read );
        
        read.clear();
        
        return bytes;
    }

    @Override
    protected boolean onReadPacketHeader( ByteBuffer in, PacketHeader packet )
    {
        return false;
    }

    @Override
    protected void onWritePacketHeader( ByteBuffer out )
    {

    }

    @Override
    protected int onWritePacketSize( ByteBuffer out )
    {
        return 0;
    }

}
