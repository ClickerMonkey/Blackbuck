package org.magnos.game.net.nio;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.magnos.game.net.ChannelProvider;

public class NioChannelProvider implements ChannelProvider
{
	
	public static final ChannelProvider INSTANCE = new NioChannelProvider();

	@Override
	public ServerSocketChannel newServerSocket() throws IOException
	{
		return ServerSocketChannel.open();
	}

	@Override
	public SocketChannel newSocket() throws IOException 
	{
		return SocketChannel.open();
	}

	@Override
	public DatagramChannel newDatagram() throws IOException 
	{
		return DatagramChannel.open();
	}
	
}
