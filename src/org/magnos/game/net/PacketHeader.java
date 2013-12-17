package org.magnos.game.net;


public class PacketHeader
{
    public int magicNumber;
    public int index;
    public long time;
    public long receivedTime;
    public int size;
}
