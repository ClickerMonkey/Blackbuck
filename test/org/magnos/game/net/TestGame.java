package org.magnos.game.net;

import org.junit.Test;
import org.magnos.game.net.tcp.TcpProtocolProvider;


public class TestGame
{

    interface UserToServer {
        void signin(String name);
        void update(float x, float y);
    }
    
    interface ServerToUser {
        void onSignInSuccess();
        void onUserSignIn(String name);
        void onUserUpdate(String name, float x, float y);
    }
    
    class GameServer implements HasClient, UserToServer {
        class GameServerUser {
            String name;
            float x, y;
        }
        Client currentClient;
        public void signin( String name ) {
            
        }
        public void update( float x, float y ) {
        }
        public void setCurrentClient( Client client ) {
            currentClient = client;
        }
    }

    @Test
    public void test()
    {
        Protocol protocol = new Protocol( 0xCAFEBABE, 1380, new TcpProtocolProvider() );
        protocol.addInterface( UserToServer.class );
        protocol.addInterface( ServerToUser.class );
        protocol.addListener( new UserToServer() {
            public void update( float x, float y ) {
                
            }
            public void signin( String name ) {
                
            }
        });
        
    }
    
}
