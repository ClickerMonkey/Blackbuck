package org.magnos.game.net;

import org.magnos.reflect.impl.ReflectMethod;

public class RemoteMethodCall
{
    public RemoteInterface remoteInterface;
    public RemoteMethod remoteMethod;
    public ReflectMethod reflectMethod;
    public Object[] arguments;
    public int callSize;
}
