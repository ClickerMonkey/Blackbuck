
package org.magnos.game.net.util;

import java.util.Iterator;

public class IterableNone<T> implements Iterable<T>, Iterator<T>
{

    @Override
    public boolean hasNext()
    {
        return false;
    }

    @Override
    public T next()
    {
        return null;
    }

    @Override
    public void remove()
    {

    }
    
    @Override
    public Iterator<T> iterator()
    {
        return this;
    }

}
