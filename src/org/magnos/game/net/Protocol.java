
package org.magnos.game.net;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;

import org.magnos.game.net.nio.NioChannelProvider;
import org.magnos.reflect.ReflectFactory;


public class Protocol
{
	
	public static ChannelProvider DEFAULT_CHANNEL_PROVIDER = NioChannelProvider.INSTANCE;

    private int magicNumber;
    private int bufferSize;
    private ProtocolProvider provider;
    private ChannelProvider channels;
    private ArrayDeque<ByteBuffer> bufferPool;

    private RemoteMethodEntry[][] entries = {};

    public Protocol( int magicNumber, int bufferSize, ProtocolProvider provider )
    {
    	this( magicNumber, bufferSize, provider, DEFAULT_CHANNEL_PROVIDER );
    }
    
    public Protocol( int magicNumber, int bufferSize, ProtocolProvider provider, ChannelProvider channels )
    {
        this.magicNumber = magicNumber;
        this.bufferSize = bufferSize;
        this.bufferPool = new ArrayDeque<ByteBuffer>();
        this.provider = provider;
        this.channels = channels;
    }

    public ByteBuffer allocateBuffer()
    {
        return (bufferPool.isEmpty() ? ByteBuffer.allocate( bufferSize ) : bufferPool.pop());
    }

    public void releaseBuffer( ByteBuffer bb )
    {
        bufferPool.offer( bb );
    }

    public Client newClient( String host, int port )
    {
        return provider.newClient( this, host, port );
    }

    public Server newServer( int port )
    {
        return provider.newServer( this, port );
    }

    public <T> Service<T> newService( Class<T> remoteInterface )
    {
        return new Service<T>( remoteInterface );
    }

    public void addInterface( Class<?> serviceInterface )
    {
        final RemoteInterface remoteInterface = serviceInterface.getAnnotation( RemoteInterface.class );
        final int interfaceId = remoteInterface.id();

        entries = ensureIndex( entries, interfaceId );

        final int methodMax = getMaxRemoteMethodId( serviceInterface );

        entries[interfaceId] = ensureIndex( entries[interfaceId], methodMax );

        for (Method method : serviceInterface.getMethods())
        {
            final RemoteMethod remoteMethod = method.getAnnotation( RemoteMethod.class );

            if (remoteMethod != null && entries[interfaceId][remoteMethod.id()] == null)
            {
                RemoteMethodEntry entry = new RemoteMethodEntry();
                entry.method = method;
                entry.remoteMethod = remoteMethod;
                entry.reflectMethod = ReflectFactory.addMethod( method );

                entries[interfaceId][remoteMethod.id()] = entry;
            }
        }
    }

    private <T> T[] ensureIndex( T[] array, int index, T ... emptyArray )
    {
        if (array == null)
        {
            array = Arrays.copyOf( emptyArray, index + 1 );
        }
        else if (index >= array.length)
        {
            array = Arrays.copyOf( array, index + 1 );
        }

        return array;
    }

    private int getMaxRemoteMethodId( Class<?> serviceInterface )
    {
        int methodMax = 0;

        for (Method method : serviceInterface.getMethods())
        {
            RemoteMethod remoteMethod = method.getAnnotation( RemoteMethod.class );

            if (remoteMethod != null)
            {
                methodMax = Math.max( methodMax, remoteMethod.id() );
            }
        }

        return methodMax;
    }

    public void addListener( Object listener )
    {
        Class<?> listenerClass = listener.getClass();

        for (Class<?> listenerInterface : listenerClass.getInterfaces())
        {
            RemoteInterface ri = listenerInterface.getAnnotation( RemoteInterface.class );

            if (ri != null)
            {
                addInterface( listenerInterface );

                for (RemoteMethodEntry entry : entries[ri.id()])
                {
                    if (entry != null)
                    {
                        entry.listener = listener;    
                    }
                }
            }
        }
    }

    public RemoteMethodEntry getEntry( int interfaceId, int methodId )
    {
        assert interfaceId >= 0;
        assert methodId >= 0;
        assert interfaceId < entries.length;
        assert entries[interfaceId] != null;
        assert methodId < entries[interfaceId].length;
        assert entries[interfaceId][methodId] != null;

        return entries[interfaceId][methodId];
    }

    public int getMagicNumber()
    {
        return magicNumber;
    }

    public int getBufferSize()
    {
        return bufferSize;
    }
    
    public ProtocolProvider getProvider()
    {
        return provider;
    }
    
    public ChannelProvider getChannels()
    {
    	return channels;
    }

}
