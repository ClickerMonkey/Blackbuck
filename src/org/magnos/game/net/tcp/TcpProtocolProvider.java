
package org.magnos.game.net.tcp;

import java.net.InetSocketAddress;

import org.magnos.game.net.Client;
import org.magnos.game.net.Protocol;
import org.magnos.game.net.ProtocolProvider;
import org.magnos.game.net.Server;


public class TcpProtocolProvider implements ProtocolProvider
{

    @Override
    public Client newClient( Protocol protocol, String host, int port )
    {
        return new TcpClient( protocol, new InetSocketAddress( host, port ) );
    }

    @Override
    public Server newServer( Protocol protocol, int port )
    {
        return new TcpServer( protocol, port );
    }

}
