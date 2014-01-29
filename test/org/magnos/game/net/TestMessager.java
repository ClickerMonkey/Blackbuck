
package org.magnos.game.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.magnos.game.net.tcp.TcpProtocolProvider;


public class TestMessager
{

    @RemoteInterface (id = 0 )
    interface UserToServer
    {
        @RemoteMethod (id = 0 ) void onSignIn( String name );
        @RemoteMethod (id = 1 ) void onMessage( String message );
        @RemoteMethod (id = 2 ) void onSignOut();
    }

    @RemoteInterface (id = 1 )
    interface ServerToUser
    {
        @RemoteMethod (id = 0 ) void onSignInSuccess();
        @RemoteMethod (id = 1 ) void onSignInFailure( String reason );
        @RemoteMethod (id = 2 ) void onUserSignIn( String name );
        @RemoteMethod (id = 3 ) void onMessage( String name, String message );
        @RemoteMethod (id = 4 ) void onUserSignOut( String name );
    }

    class ServerUser
    {
        String name;
        Client client;
        ServerToUser service;
    }

    class MessageServer implements HasClient, UserToServer
    {

        Client currentClient;
        Map<String, ServerUser> userMap = new HashMap<String, ServerUser>();
        Service<ServerToUser> globalService;

        public void setCurrentClient( Client client )
        {
            currentClient = client;
        }

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
    }

    class MessageClient implements ServerToUser
    {

        public void onSignInSuccess()
        {
            System.out.println( "CLIENT: sign-in success!" );
        }

        public void onSignInFailure( String reason )
        {
            System.out.println( "CLIENT: sign-in failure: " + reason );
        }

        public void onUserSignIn( String name )
        {
            System.out.println( "CLIENT: user " + name + " has signed in" );
        }

        public void onMessage( String name, String message )
        {
            System.out.println( "CLIENT: " + name + ": " + message );
        }

        public void onUserSignOut( String name )
        {
            System.out.println( "CLIENT: sign-out: " + name );
        }
    }

    @Test
    public void test() throws Exception
    {
        final int PORT = 4550;
        final long CLIENT_UPDATE_RATE = 200;
        final long SERVER_UPDATE_RATE = 200;
        final long SERVER_IDLE_SLEEP = 1;
        final Protocol protocol = new Protocol( 0xCAFEBABE, 1380, new TcpProtocolProvider() );
        protocol.addInterface( UserToServer.class );
        protocol.addInterface( ServerToUser.class );
        protocol.addListener( new MessageServer() );
        protocol.addListener( new MessageClient() );

        // Server
        new Thread( new Runnable() {

            public void run()
            {
                try
                {
                    Server server = protocol.newServer( PORT );
                    server.setServerListener( new ServerListenerAdapter() {

                        public void onClientConnect( Server server, Client client )
                        {
                            System.out.println( "SERVER: client connect on " + client.getAddress() );
                        }

                        public void onAcceptError( Server server, Exception e )
                        {
                            System.out.println( "SERVER: client accept error: " );
                            if (e != null) e.printStackTrace();
                        }

                        public void onClientClose( Server server, Client client, Exception e )
                        {
                            System.out.println( "SERVER: client close: " + client.getAddress() );
                            if (e != null) e.printStackTrace();
                        }
                    } );
                    server.init();
                    server.setClientUpdateRate( SERVER_UPDATE_RATE );
                    server.setIdleSleepMillis( SERVER_IDLE_SLEEP );
                    for (;;)
                    {
                        server.update();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        Thread.sleep( 1000 );

        // Client
        final Client client = protocol.newClient( "localhost", PORT );
        client.setUpdateRate( CLIENT_UPDATE_RATE );
        client.init();

        final UserToServer service = client.newService( UserToServer.class );
        service.onSignIn( "ClickerMonkey" );
        service.onMessage( "Hello World!" );
        
        final AtomicBoolean closeFlag = new AtomicBoolean( false );

        new Thread( new Runnable() {

            public void run()
            {
                try
                {
                    Scanner in = new Scanner( System.in );
                    
                    while (in.hasNextLine())
                    {
                        service.onMessage( in.nextLine() );
                    }
                    
                    service.onSignOut();
                    
                    closeFlag.set( true );
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        while (!client.isClosed())
        {
            if (closeFlag.get())
            {
                client.sendAllNow();
                client.close();
            }
            else
            {
                client.read();
                client.update();
                
                if (client.isReadyToSend())
                {
                    client.send();
                    
                    if (client.getCallsSent() > 0)
                    {
                        System.out.println( "CLIENT: calls sent: " + client.getCallsSent() );
                    }
                }
            }
        }
    }

}
