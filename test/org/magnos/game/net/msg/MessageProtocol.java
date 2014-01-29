
package org.magnos.game.net.msg;

import org.magnos.game.net.Protocol;
import org.magnos.game.net.tcp.TcpProtocolProvider;


public class MessageProtocol
{

    public static final int BUFFER_SIZE = 1380;
    public static final int MAGIC_NUMBER = 0xBABEBABE;
    public static final int PORT = 4550;
    public static final String HOST = "localhost";
    public static final long CLIENT_UPDATE_RATE = 200;
    public static final long SERVER_UPDATE_RATE = 200;
    public static final long SERVER_IDLE_SLEEP = 1;
    public static final Protocol PROTOCOL;

    static
    {
        PROTOCOL = new Protocol( MAGIC_NUMBER, BUFFER_SIZE, new TcpProtocolProvider() );
        PROTOCOL.addInterface( UserToServer.class );
        PROTOCOL.addInterface( ServerToUser.class );
    }

}
