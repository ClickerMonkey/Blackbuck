
package org.magnos.game.net;

public interface ServerListener
{

    public void onInit( Server server );

    public void onUpdateBegin( Server server );

    public void onUpdateEnd( Server server );

    public void onClientConnect( Server server, Client client );

    public void onClientFailedConnect( Server server, Client client, Exception e );

    public void onClientUpdate( Server server, Client client );

    public void onClientClose( Server server, Client client, Exception e );

    public void onAcceptError( Server server, Exception e );
}
