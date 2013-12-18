package org.magnos.game.net.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.magnos.game.net.AbstractClient;
import org.magnos.game.net.Protocol;
import org.magnos.game.net.RemoteMethodCall;


public class UdpClient extends AbstractClient
{
    public static final int HEADER_SIZE = 32;
    public static final int HEADER_PACKET_SIZE_OFFSET = 28;

    public DatagramChannel channel;
    public int readAck;
    public int readAckSequence;
    public int writeAck;
    public int writeAckSequence;
    
    public UdpClient( Protocol protocol, InetSocketAddress address )
    {
        super( protocol, null, address );
    }

    @Override
    protected void onInit() throws IOException
    {
        channel = DatagramChannel.open();
        channel.configureBlocking( false );
        channel.connect( address );
    }

    @Override
    protected void onUpdate() throws IOException
    {
        write();
    }

    @Override
    protected void onClose() throws IOException
    {
        channel.close();
    }

    @Override
    protected void onWrite( ByteBuffer packet ) throws IOException
    {
        channel.write( packet );
    }

    @Override
    protected int onRead( ByteBuffer out ) throws IOException
    {
        return channel.read( out );
    }

    @Override
    protected void onCallWrite( RemoteMethodCall call )
    {
        // keep track of call depending on retryCount and reliable
    }

    @Override
    protected int onReadPacketHeader( ByteBuffer in )
    {
        if (in.remaining() < HEADER_SIZE)
        {
            return ON_READ_REWIND;
        }
        
        int magicNumber = in.getInt();
        
        if (magicNumber != protocol.getMagicNumber())
        {
            return ON_READ_CLOSE;
        }

        long packetTime = in.getLong();
        long receivedTime = in.getLong();
        int ack = in.getInt(); // this is the packet index the other last received
        int ackSequence = in.getInt(); // this is the bitset of previous packets they received
        int packetSize = in.getInt();
        
        if (in.remaining() < packetSize)
        {
            return ON_READ_REWIND;
        }
        
        pingTime = System.nanoTime() - receivedTime;
        lastReceivedPacketSize = packetSize;
        lastReceivedPacketTime = packetTime;
        readAck = ack;
        readAckSequence = ackSequence;
        
        // use ack and acksequence to remove any pending calls
        // if ack is a jump from the last packet index sent, add the calls in between to out-bound
        // if packetIndex is one we already received, jump to end of packet and ignore all calls
        // if packetIndex is one we were missing, check for complete call sequences for ordered channels

        return packetSize;
    }
    
    @Override
    protected void onWritePacketHeader( ByteBuffer out )
    {
        out.putInt( protocol.getMagicNumber() );
        out.putInt( packetIndex ); // this is MY packet index, please acknowledge
        out.putLong( System.nanoTime() );
        out.putLong( lastReceivedPacketTime );
        out.putInt( writeAck ); // hey this is the index of the last packet we received from you
        out.putInt( writeAckSequence ); // this is the bitset of previous packets we've received from you
        out.putInt( 0 );
    }

    @Override
    protected int onWritePacketSize( ByteBuffer out )
    {
        int packetSize = out.limit() - HEADER_SIZE;

        out.putInt( HEADER_PACKET_SIZE_OFFSET, packetSize );

        return packetSize;
    }

}
