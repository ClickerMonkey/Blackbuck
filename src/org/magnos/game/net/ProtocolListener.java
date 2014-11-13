package org.magnos.game.net;

public interface ProtocolListener 
{
	public boolean onReadBlock(Client client, Match match, int expectedStates, int actualStates);
	public boolean onWriteBlock(Client client, Match match, int expectedStates, int actualStates);
}
