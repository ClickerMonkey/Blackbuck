
package org.magnos.game.net.msg;

import org.magnos.game.net.RemoteInterface;
import org.magnos.game.net.RemoteMethod;

@RemoteInterface (id = 1 )
public interface ServerToUser
{

    @RemoteMethod (id = 0 )
    public void onSignInSuccess();

    @RemoteMethod (id = 1 )
    public void onSignInFailure( String reason );

    @RemoteMethod (id = 2 )
    public void onUserSignIn( String name );

    @RemoteMethod (id = 3 )
    public void onMessage( String name, String message );

    @RemoteMethod (id = 4 )
    public void onUserSignOut( String name );
}
