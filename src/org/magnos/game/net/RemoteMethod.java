
package org.magnos.game.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention (RetentionPolicy.RUNTIME )
@Target ({ ElementType.METHOD } )
public @interface RemoteMethod
{
    public int id();

    public int priority() default 5;
    
    public int channel() default 0;

    public boolean reliable() default true;

    public boolean ordered() default true;

    public int retryCount() default 1;

    public int readStates() default -1;

    public Match readMatch() default Match.ALWAYS;

    public int writeStates() default -1;

    public Match writeMatch() default Match.ALWAYS;
}
