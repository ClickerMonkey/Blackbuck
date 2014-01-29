
package org.magnos.game.net.msg;

import java.util.HashMap;
import java.util.Map;

import org.magnos.game.net.Client;
import org.magnos.game.net.HasClient;
import org.magnos.game.net.Server;
import org.magnos.game.net.ServerListener;

public class ServerImplementation  implements HasClient, UserToServer, ServerListener
{

    public Client currentClient;
    public Map<String, ServerUser> userMap = new HashMap<String, ServerUser>();

    @Override
    public void setCurrentClient( Client client )
    {
        currentClient = client;
    }
    
    @Override
    public void onSignIn( String name )
    {
        ServerToUser service = currentClient.newService( ServerToUser.class );
        if (userMap.containsKey( name ))
        {
            System.out.println( "SERVER: sign-in attempt failed for " + name + ", user already signed in" );
            service.onSignInFailure( "A user with that name already exists" );
        }
        else
        {
            System.out.println( "SERVER: sign-in attempt for " + name );
            ServerUser u = new ServerUser();
            currentClient.attach( u );
            u.name = name;
            u.client = currentClient;
            u.service = service;

            service.onSignInSuccess();

            for (ServerUser otherUser : userMap.values())
            {
                otherUser.service.onUserSignIn( name );
                service.onUserSignIn( otherUser.name );
            }

            userMap.put( name, u );
        }
    }

    @Override
    public void onMessage( String message )
    {
        System.out.println( "SERVER: " + message );
        ServerUser u = currentClient.attachment();

        if (u == null)
        {
            currentClient.close();
        }
        else
        {
            for (ServerUser otherUser : userMap.values())
            {
                otherUser.service.onMessage( u.name, message );
            }
        }
    }

    @Override
    public void onSignOut()
    {
        ServerUser u = currentClient.attachment();
        currentClient.close();

        if (u != null)
        {
            System.out.println( "SERVER: sign-out: " + u.name );

            userMap.remove( u.name );

            for (ServerUser otherUser : userMap.values())
            {
                otherUser.service.onUserSignOut( u.name );
            }
        }
        else
        {
            System.out.println( "SERVER: sign-out of unknown user" );
        }
    }

    @Override
    public void onClientConnect( Server server, Client client )
    {
        System.out.println( "SERVER: client connect on " + client.getAddress() );
    }

    @Override
    public void onAcceptError( Server server, Exception e )
    {
        System.out.println( "SERVER: client accept error: " );
        
        if (e != null) e.printStackTrace();
    }

    @Override
    public void onClientClose( Server server, Client client, Exception e )
    {
        System.out.println( "SERVER: client close: " + client.getAddress() );
        
        if (e != null) e.printStackTrace();
    }

    @Override
    public void onInit( Server server )
    {
        System.out.println( "SERVER: onInit on port: " + server.getPort() );
    }

    @Override
    public void onUpdateBegin( Server server )
    {
        
    }

    @Override
    public void onUpdateEnd( Server server )
    {
        
    }

    @Override
    public void onClientFailedConnect( Server server, Client client, Exception e )
    {
        System.out.println( "SERVER: client failed to connect: " + client.getAddress() );
        
        if (e != null) e.printStackTrace();
    }

    @Override
    public void onClientUpdate( Server server, Client client )
    {
        
    }

}
