package org.magnos.game.net;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;


public class Protocol
{
    public static final int HEADER_SIZE = 16;
    public static final int CALL_HEADER_SIZE = 2;
    
    private int bufferSize;
    private ArrayDeque<ByteBuffer> bufferPool;
    private Object[] listeners = new Object[256];
    
    public Protocol(int bufferSize) {
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
    
    public void addListener(Object listener) {
        Class<?> listenerClass = listener.getClass();
        for (Class<?> listenerInterface : listenerClass.getInterfaces()) {
            RemoteInterface ri = listenerInterface.getAnnotation( RemoteInterface.class );
            if (ri != null) {
                listeners[ ri.id() ] = listener;
            }
        }
    }
    
    public int getMaximumCallSize()
    {
        return bufferSize - HEADER_SIZE - CALL_HEADER_SIZE;
    }
    
    
}
