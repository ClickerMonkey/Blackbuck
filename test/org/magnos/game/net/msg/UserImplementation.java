
package org.magnos.game.net.msg;

public class UserImplementation implements ServerToUser
{

    public void onSignInSuccess()
    {
        System.out.println( "USER: sign-in success!" );
    }

    public void onSignInFailure( String reason )
    {
        System.out.println( "USER: sign-in failure: " + reason );
    }

    public void onUserSignIn( String name )
    {
        System.out.println( "USER: user " + name + " has signed in" );
    }

    public void onMessage( String name, String message )
    {
        System.out.println( "USER: " + name + ": " + message );
    }

    public void onUserSignOut( String name )
    {
        System.out.println( "USER: sign-out: " + name );
    }
}
