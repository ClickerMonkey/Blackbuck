
package org.magnos.game.net.msg;

import java.util.Scanner;

import org.magnos.game.net.Client;
import org.magnos.game.net.Protocol;

public class UserMain
{

    public static void main( String[] args ) throws Exception
    {
        UserImplementation userImplementation = new UserImplementation();

        Protocol protocol = MessageProtocol.PROTOCOL;
        protocol.addListener( userImplementation );
        
        Client client = protocol.newClient( MessageProtocol.HOST, MessageProtocol.PORT );
        client.setUpdateRate( MessageProtocol.CLIENT_UPDATE_RATE );
        client.init();

        UserToServer service = client.newService( UserToServer.class );
        
        Scanner inputName = new Scanner( System.in );
        System.out.print( "Name > ");
        service.onSignIn( inputName.nextLine() );
        
        MessageFeed feed = new MessageFeed();
        
        while (!client.isClosed())
        {
            if (feed.isFinished())
            {
                service.onSignOut();
                client.sendAllNow();
                client.close();
            }
            else
            {
                for (String m = feed.nextMessage(); m != null; m = feed.nextMessage())
                {
                    service.onMessage( m );
                }
                
                client.read();
                client.update();
                
                if (client.isReadyToSend())
                {
                    client.send();
                }
            }
        }
    }

}
