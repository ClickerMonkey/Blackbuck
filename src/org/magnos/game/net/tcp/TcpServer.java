
package org.magnos.game.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.magnos.game.net.AbstractServer;
import org.magnos.game.net.Client;
import org.magnos.game.net.Protocol;


public class TcpServer extends AbstractServer
{

    public ServerSocketChannel socket;

    public TcpServer( Protocol protocol, int port )
    {
        super( protocol, port );
    }
    
    @Override
    protected void onInit() throws IOException
    {
    	socket = protocol.getChannels().newServerSocket();
        socket.socket().bind( new InetSocketAddress( port ) );
        socket.configureBlocking( false );
    }

    @Override
    protected void onClose() throws IOException
    {
        socket.close();    
    }

    @Override
    protected Client tryAccept() throws IOException
    {
        SocketChannel client = socket.accept();
        
        return ( client == null ? null : new TcpClient( protocol, this, client ) );
    }

}
