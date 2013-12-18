
package org.magnos.game.net;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.magnos.game.net.util.IterableNone;
import org.magnos.reflect.ReflectFactory;
import org.magnos.reflect.impl.ReflectMethod;


public class Service<T> implements InvocationHandler
{

	public static final Iterable<Client> NO_TARGETS = new IterableNone<Client>();

	private class MethodMeta
	{
		private RemoteMethod remoteMethod;
		private ReflectMethod reflectMethod;
	}

	private Iterable<Client> targetClients;
	private final Class<T> interfaceClass;
	private final RemoteInterface remoteInterface;
	private final Map<Method, MethodMeta> remoteMethods;
	private final T proxy;

	@SuppressWarnings ("unchecked" )
	public Service( Class<T> interfaceClass )
	{
		this.interfaceClass = interfaceClass;
		this.remoteInterface = interfaceClass.getAnnotation( RemoteInterface.class );
		this.remoteMethods = getRemoteMethods( interfaceClass );
		this.proxy = (T)Proxy.newProxyInstance( getClass().getClassLoader(), new Class[] { interfaceClass }, this );
		this.targetClients = NO_TARGETS;
	}

	@Override
	public Object invoke( Object object, Method method, Object[] arguments ) throws Throwable
	{
		assert object.getClass() == interfaceClass;
		assert targetClients != null;

		final MethodMeta meta = remoteMethods.get( method );

		if (meta != null)
		{
		    final RemoteMethodCall call = new RemoteMethodCall();
			call.remoteInterface = remoteInterface;
			call.remoteMethod = meta.remoteMethod;
			call.reflectMethod = meta.reflectMethod;
			call.callSize = meta.reflectMethod.sizeOf( arguments );
			call.arguments = arguments;
			call.timestamp = System.nanoTime();

			final Match writeMatch = meta.remoteMethod.writeMatch();
			final int writeStates = meta.remoteMethod.writeStates();
			final MismatchAction writeAction = meta.remoteMethod.writeMismatch();
			
			for (Client client : targetClients)
			{
				if (writeMatch.isMatch( writeStates, client.getStates() ))
				{
					client.queue( call );
				}
				else
				{
					switch (writeAction) 
					{
					case CLOSE:
					    client.close();
					    break;
					case LOG:
					    System.out.format( "Tried to send %s.%s to client at %s but they are not in the correct state (%d with match %s) they have the state %d.", interfaceClass.getSimpleName(), method.getName(), client.getAddress(), writeStates, writeMatch, client.getStates() );
						break;
					case NOTHING:
					    break;
					}
				}
			}
		}
		else
		{
		    return method.invoke( object, arguments );
		}

		return null;
	}

	private Map<Method, MethodMeta> getRemoteMethods( Class<?> c )
	{
		Map<Method, MethodMeta> map = new HashMap<Method, MethodMeta>();

		for (Method m : c.getMethods())
		{
			RemoteMethod rm = m.getAnnotation( RemoteMethod.class );

			if (rm != null)
			{
				MethodMeta meta = new MethodMeta();
				meta.reflectMethod = ReflectFactory.addMethod( m );
				meta.remoteMethod = rm;
				map.put( m, meta );
			}
		}

		return map;
	}

	public T get()
	{
		return proxy;
	}

	public Iterable<Client> getTargets()
	{
		return targetClients;
	}

	public void setTarget( Iterable<Client> targetClients )
	{
		this.targetClients = targetClients;
	}

	public Class<?> getRemoteInterfaceClass()
	{
		return interfaceClass;
	}

	public RemoteInterface getRemoteInterface()
	{
		return remoteInterface;
	}

	public Set<Method> getRemoteMethods()
	{
		return remoteMethods.keySet();
	}

	public ReflectMethod getReflectMethod( Method m )
	{
		MethodMeta meta = remoteMethods.get( m );

		return (meta == null ? null : meta.reflectMethod);
	}

	public RemoteMethod getRemoteMethod( Method m )
	{
		MethodMeta meta = remoteMethods.get( m );

		return (meta == null ? null : meta.remoteMethod);
	}

}
