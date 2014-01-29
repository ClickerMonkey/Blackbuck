
package org.magnos.game.net;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public abstract class AbstractServer implements Server
{

    protected final int port;
    protected final Protocol protocol;

    protected boolean initialized;
    protected boolean closed;
    protected ServerListener listener;

    protected Set<Client> clients;

    protected long idleSleepMillis = 1;
    protected long clientUpdateRate;

    public AbstractServer( Protocol protocol, int port )
    {
        this.protocol = protocol;
        this.port = port;
        this.initialized = false;
        this.closed = true;
    }

    protected abstract void onInit() throws IOException;
    protected abstract void onClose() throws IOException;

    protected abstract Client tryAccept() throws IOException;

    private boolean isNotReady()
    {
        return closed || !initialized || listener == null;
    }

    @Override
    public void init() throws IOException
    {
        try
        {
            onInit();

            initialized = true;
            closed = false;
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw e;
        }

        if (initialized)
        {
            clients = new HashSet<Client>();
        }
    }

    @Override
    public boolean isInitialized()
    {
        return initialized;
    }

    @Override
    public void update() throws IOException
    {
        if (isNotReady())
        {
            return;
        }

        listener.onUpdateBegin( this );

        boolean sleep = true;

        try
        {
            Client acceptedClient = null;

            while ((acceptedClient = tryAccept()) != null)
            {
                if (initializeClient( acceptedClient ))
                {
                    sleep = false;
                }
            }
        }
        catch (Exception e)
        {
            listener.onAcceptError( this, e );
        }

        Iterator<Client> iter = clients.iterator();

        while (iter.hasNext())
        {
            Client c = iter.next();

            if (!updateClient( c ))
            {
                iter.remove();
            }
            else if ((c.getPacketsRead() > 0) || (c.getCallsSent() > 0))
            {
                sleep = false;
            }
        }

        listener.onUpdateEnd( this );

        if (sleep && idleSleepMillis > 0)
        {
            try
            {
                Thread.sleep( idleSleepMillis );
            }
            catch (Exception e)
            {
                // ignore
            }
        }
    }

    private boolean initializeClient( Client c )
    {
        try
        {
            c.init();
            c.setUpdateRate( clientUpdateRate );

            listener.onClientConnect( this, c );
            clients.add( c );

            return true;
        }
        catch (Exception e)
        {
            listener.onClientFailedConnect( this, c, e );

            return false;
        }
    }

    private boolean updateClient( Client c )
    {
        if (c.isClosed())
        {
            listener.onClientClose( this, c, null );

            return false;
        }

        try
        {
            c.read();
            c.update();

            listener.onClientUpdate( this, c );

            if (c.isReadyToSend())
            {
                c.send();
            }

            return true;
        }
        catch (Exception e)
        {
            listener.onClientClose( this, c, e );

            c.close();

            return false;
        }
    }

    @Override
    public void close()
    {
        if (isNotReady())
        {
            return;
        }
        
        for (Client c : clients)
        {
            c.close();
        }

        try
        {
            onClose();
        }
        catch (Exception e)
        {
            // ignore
        }

        clients.clear();
        closed = true;
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public int getPort()
    {
        return port;
    }

    @Override
    public Set<Client> getClients()
    {
        return clients;
    }

    @Override
    public ServerListener getServerListener()
    {
        return listener;
    }

    @Override
    public void setServerListener( ServerListener listener )
    {
        this.listener = listener;
    }

    public Protocol getProtocol()
    {
        return protocol;
    }

    public long getIdleSleepMillis()
    {
        return idleSleepMillis;
    }

    public void setIdleSleepMillis( long sleep )
    {
        this.idleSleepMillis = sleep;
    }
    
    public long getClientUpdateRate()
    {
        return clientUpdateRate;
    }
    
    public void setClientUpdateRate(long updateRate)
    {
        this.clientUpdateRate = updateRate;
    }

}
