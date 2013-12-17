
package org.magnos.game.net;



public interface ProtocolProvider
{

    public Client newClient( Protocol protocol, String host, int port );

    public Server newServer( Protocol protocol, int port );

}
