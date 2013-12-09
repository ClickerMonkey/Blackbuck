package org.magnos.game.net;

import java.lang.reflect.Method;

import org.magnos.reflect.impl.ReflectMethod;

public class RemoteMethodEntry
{
	public ReflectMethod reflectMethod;
	public Method method;
	public Object listener;
}
