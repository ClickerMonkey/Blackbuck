package org.magnos.game.net;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;

import org.magnos.reflect.ReflectFactory;


public class Protocol
{
    public static final int HEADER_SIZE = 16;
    public static final int HEADER_PACKET_SIZE_OFFSET = 24;
    
    private int magicNumber;
    private int bufferSize;
    private ArrayDeque<ByteBuffer> bufferPool;
    
    private RemoteMethodEntry[][] entries = {};
    
    public Protocol(int magicNumber, int bufferSize) {
    	this.magicNumber = magicNumber;
        this.bufferSize = bufferSize;
        this.bufferPool = new ArrayDeque<ByteBuffer>();
    }
    
    public ByteBuffer allocateBuffer() {
        return (bufferPool.isEmpty() ? ByteBuffer.allocate( bufferSize ) : bufferPool.pop());
    }
    public void releaseBuffer(ByteBuffer bb) { 
        bufferPool.offer( bb );
    }
    
    public Client newClient(String host, int port) {
        return null;
    }
    
    public Server newServer(int port) {
        return null;
    }
    
    public <T> Service<T> newService(Class<T> remoteInterface) {
        return null;
    }
    
    public void addInterface(Class<?> serviceInterface) 
    {
    	RemoteInterface remoteInterface = serviceInterface.getAnnotation( RemoteInterface.class );
    	int interfaceId = remoteInterface.id();
    	
    	if (entries.length <= interfaceId) 
    	{
    		entries = Arrays.copyOf( entries, interfaceId + 1 );
    	}
    	
    	int methodMax = 0;
    	
    	for (Method method : serviceInterface.getMethods()) 
    	{
    		RemoteMethod remoteMethod = method.getAnnotation( RemoteMethod.class );
    		
    		if (remoteMethod != null) 
    		{
    			methodMax = Math.max( methodMax, remoteMethod.id() );	
    		}
    	}
    	
    	if (entries[interfaceId] == null) 
    	{
    		entries[interfaceId] = new RemoteMethodEntry[methodMax + 1];
    	} 
    	else if (entries[interfaceId].length <= methodMax) 
    	{
    		entries[interfaceId] = Arrays.copyOf( entries[interfaceId], methodMax + 1 );
    	}
    	
    	for (Method method : serviceInterface.getMethods()) 
    	{
    		RemoteMethod remoteMethod = method.getAnnotation( RemoteMethod.class );
    		
    		if (remoteMethod != null && entries[interfaceId][remoteMethod.id()] == null) 
    		{
    			RemoteMethodEntry entry = new RemoteMethodEntry();
    			entry.method = method;
    			entry.reflectMethod = ReflectFactory.addMethod( method );
    			
    			entries[interfaceId][remoteMethod.id()] = entry;
    		}
    	}
    }

    public void addListener(Object listener) 
    {
        Class<?> listenerClass = listener.getClass();
        
        for (Class<?> listenerInterface : listenerClass.getInterfaces()) 
        {
            RemoteInterface ri = listenerInterface.getAnnotation( RemoteInterface.class );
            
            if (ri != null) 
            {
            	addInterface( listenerInterface );
            	
            	for (RemoteMethodEntry entry : entries[ ri.id() ])
            	{
            		entry.listener = listener;
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
    	
    	return entries[ interfaceId ][ methodId ];
    }
    
    protected void execute(int interfaceId, int methodId, Object[] arguments)
    {
    	
    }
    
    public int getMaximumCallSize()
    {
        return bufferSize - HEADER_SIZE - 10;
    }
    
    public int getMagicNumber()
    {
    	return magicNumber;
    }
    
    
}
