package org.magnos.game.net.msg;

import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MessageFeed implements Runnable
{
    
    public static final String QUIT = "quit";

    public boolean finished;
    public Queue<String> messages;
    public Thread thread;
    
    public MessageFeed()
    {
        this.messages = new ConcurrentLinkedQueue<String>();
        this.thread = new Thread( this );
        this.thread.start();
    }
    
    public boolean isFinished()
    {
        return finished;
    }
    
    public String nextMessage()
    {
        return messages.poll();
    }
    
    public void run()
    {
        Scanner in = new Scanner( System.in );
        
        while (in.hasNextLine())
        {
            String line = in.nextLine();
            
            if (line.equalsIgnoreCase( QUIT ))
            {
                finished = true;
                break;
            }
            else
            {
                messages.add( line );
            }
        }
        
        finished = true;
    }
    
}
