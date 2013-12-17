package org.magnos.game.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Queue;


public interface Client
{
    public void init() throws IOException;
    public boolean isInitialized();
    public void read() throws IOException;
    public void update() throws IOException;
    public boolean isReadyToSend();
    public void queue(RemoteMethodCall call);
    public void send() throws IOException;
    public void sendAllNow() throws IOException;
    public void close();
    public boolean isClosed();

    public Server getServer();
    public Protocol getProtocol();

    public <T> T newService(Class<T> remoteInterface);
    
    public InetSocketAddress getAddress();
    public Queue<RemoteMethodCall> getOutbound();
    public Queue<RemoteMethodCall> getInbound();
    public long getUpdateRate();
    public void setUpdateRate(long millis);
    public long getLastUpdateTime();
    public int getCallsSent();
    public int getPacketsRead();
    public int getPacketIndex();
    public int getLastPacketSize();
    public long getReadNanos();
    public long getUpdateNanos();
    public long getWriteNanos();
    public long getSendNanos();

    public void attach(Object attachment);
    public <T> T attachment();
    
    public int getStates();
    public void setStates(int states);
    public void addState(int state);
    public void removeState(int state);
    
}
