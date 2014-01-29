
package org.magnos.game.net.msg;

import org.magnos.game.net.RemoteInterface;
import org.magnos.game.net.RemoteMethod;


@RemoteInterface (id = 0 )
public interface UserToServer
{

    @RemoteMethod (id = 0 )
    public void onSignIn( String name );

    @RemoteMethod (id = 1 )
    public void onMessage( String message );

    @RemoteMethod (id = 2 )
    public void onSignOut();
    
}
