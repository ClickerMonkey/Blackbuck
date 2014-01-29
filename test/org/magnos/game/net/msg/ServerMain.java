
package org.magnos.game.net.msg;

import org.magnos.game.net.Protocol;
import org.magnos.game.net.Server;


public class ServerMain
{

    public static void main( String[] args ) throws Exception
    {
        ServerImplementation serverImplementation = new ServerImplementation();

        Protocol protocol = MessageProtocol.PROTOCOL;
        protocol.addListener( serverImplementation );

        Server server = protocol.newServer( MessageProtocol.PORT );

        server.setServerListener( serverImplementation );
        server.setClientUpdateRate( MessageProtocol.SERVER_UPDATE_RATE );
        server.setIdleSleepMillis( MessageProtocol.SERVER_IDLE_SLEEP );

        server.init();

        while (!server.isClosed())
        {
            server.update();
        }
    }

}
