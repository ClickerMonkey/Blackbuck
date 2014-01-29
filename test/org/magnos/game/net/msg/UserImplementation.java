
package org.magnos.game.net.msg;

public class UserImplementation implements ServerToUser
{

    @Override
    public void onSignInSuccess()
    {
        System.out.println( "USER: sign-in success!" );
    }

    @Override
    public void onSignInFailure( String reason )
    {
        System.out.println( "USER: sign-in failure: " + reason );
    }

    @Override
    public void onUserSignIn( String name )
    {
        System.out.println( "USER: user " + name + " has signed in" );
    }

    @Override
    public void onMessage( String name, String message )
    {
        System.out.println( "USER: " + name + ": " + message );
    }

    @Override
    public void onUserSignOut( String name )
    {
        System.out.println( "USER: sign-out: " + name );
    }
    
}
