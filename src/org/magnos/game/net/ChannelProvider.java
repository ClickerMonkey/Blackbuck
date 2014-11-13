package org.magnos.game.net;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public interface ChannelProvider 
{
	public ServerSocketChannel newServerSocket() throws IOException;
	public SocketChannel newSocket() throws IOException;
	public DatagramChannel newDatagram() throws IOException;
}
