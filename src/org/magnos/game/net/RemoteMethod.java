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

    public boolean reliable() default true;

    public boolean ordered() default true;

    public int retryCount() default 1;

    public int channel() default 0;
    
    public int readStates() default -1;
    
    public Match readMatch() default Match.ANY_OF;
    
    public MismatchAction readMismatch() default MismatchAction.LOG;
    
    public int writeStates() default -1;
    
    public Match writeMatch() default Match.ANY_OF;
    
    public MismatchAction writeMismatch() default MismatchAction.LOG;
}
