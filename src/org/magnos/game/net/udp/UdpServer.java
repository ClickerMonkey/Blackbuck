package org.magnos.game.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;

import org.magnos.game.net.AbstractServer;
import org.magnos.game.net.Client;
import org.magnos.game.net.Protocol;


public class UdpServer extends AbstractServer
{

    public DatagramChannel channel;
    public Map<SocketAddress, UdpServerClient> clientMap;
    public ByteBuffer packet;
    
    public UdpServer( Protocol protocol, int port )
    {
        super( protocol, port );
        
        this.clientMap = new HashMap<SocketAddress, UdpServerClient>();
        this.packet = protocol.allocateBuffer();
    }
    
    @Override
    protected void onInit() throws IOException
    {
    	channel = protocol.getChannels().newDatagram();
        channel.socket().bind( new InetSocketAddress( port ) );
        channel.configureBlocking( false );
    }

    @Override
    protected void onClose() throws IOException
    {
        channel.close();
    }

    @Override
    protected Client tryAccept() throws IOException
    {
        packet.clear();
        
        InetSocketAddress address = (InetSocketAddress)channel.receive( packet );
        UdpServerClient newClient = null;
        
        if (address != null)
        {
            UdpServerClient c = clientMap.get( address );
            
            if (c == null)
            {
                c = new UdpServerClient( protocol, this, address );
                clientMap.put( address, c );
                newClient = c;
            }
            
            c.read = packet;
        }
        
        return newClient;
    }

}
