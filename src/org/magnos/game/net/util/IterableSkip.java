
package org.magnos.game.net.util;

import java.util.Iterator;

public class IterableSkip<T> implements Iterable<T>, Iterator<T>
{

    private final T skip;
    private final Iterable<T> iterable;
    private Iterator<T> iterator;
    private T next;
    
    public IterableSkip(T skip, Iterable<T> iterable)
    {
        this.skip = skip;
        this.iterable = iterable;
        this.iterator = iterable.iterator();
    }
    
    @Override
    public boolean hasNext()
    {
        return next != null;
    }

    @Override
    public T next()
    {
        T current = next;

        while (iterator.hasNext())
        {
            next = iterator.next();
            
            if (next != skip)
            {
                break;
            }
        }
        
        if (!iterator.hasNext())
        {
            next = null;
        }
        
        return current;
    }

    @Override
    public void remove()
    {
        if (next != null)
        {
            iterator.remove();    
        }
    }

    public Iterator<T> reset()
    {
        iterator = iterable.iterator();
        
        next();
        
        return this;
    }
    
    @Override
    public Iterator<T> iterator()
    {
        return hasNext() ? new IterableSkip<T>( skip, iterable ) : this;
    }

}
