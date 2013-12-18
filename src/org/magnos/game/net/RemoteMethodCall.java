package org.magnos.game.net;

import org.magnos.reflect.impl.ReflectMethod;

public class RemoteMethodCall implements Comparable<RemoteMethodCall>
{
    
    public RemoteInterface remoteInterface;
    public RemoteMethod remoteMethod;
    public ReflectMethod reflectMethod;
    public Object[] arguments;
    public int callSize;
    public long timestamp;
    
    @Override
    public int compareTo( RemoteMethodCall o )
    {
        int p0 = (remoteMethod == null ? 0 : remoteMethod.priority());
        int p1 = (o.remoteMethod == null ? 0 : o.remoteMethod.priority());
        int d0 = p1 - p0;
        long d1 = Long.signum( timestamp - o.timestamp );

        return (d0 != 0 ? d0 : (int)d1 );
    }
    
}
