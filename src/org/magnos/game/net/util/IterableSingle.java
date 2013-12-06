
package org.magnos.game.net.util;

import java.util.Iterator;

public class IterableSingle<T> implements Iterable<T>, Iterator<T>
{

    private final T item;
    private boolean iterated;
    
    public IterableSingle(T item)
    {
        this.item = item;
        this.iterated = true;
    }
    
    @Override
    public boolean hasNext()
    {
        return !iterated;
    }

    @Override
    public T next()
    {
        iterated = true;
        
        return item;
    }

    @Override
    public void remove()
    {
        // ignore
    }

    public Iterator<T> reset()
    {
        iterated = false;
        
        return this;
    }
    
    @Override
    public Iterator<T> iterator()
    {
        return iterated ? reset() : new IterableSingle<T>( item );
    }

}
