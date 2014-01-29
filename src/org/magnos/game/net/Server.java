
package org.magnos.game.net;

import java.io.IOException;
import java.util.Set;


public interface Server
{
    public void init() throws IOException;
    public boolean isInitialized();
    public void update() throws IOException;
    public void close();
    public boolean isClosed();

    public int getPort();

    public Set<Client> getClients();

    public ServerListener getServerListener();
    public void setServerListener( ServerListener listener );
    
    public Protocol getProtocol();
    
    public long getIdleSleepMillis();
    public void setIdleSleepMillis(long sleep);
    
    public long getClientUpdateRate();
    public void setClientUpdateRate(long updateRate);
}
