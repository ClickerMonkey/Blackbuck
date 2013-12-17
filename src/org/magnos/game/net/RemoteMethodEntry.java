package org.magnos.game.net;

import java.lang.reflect.Method;

import org.magnos.reflect.impl.ReflectMethod;

class RemoteMethodEntry
{
	public ReflectMethod reflectMethod;
	public RemoteMethod remoteMethod;
	public Method method;
	public Object listener;
}
