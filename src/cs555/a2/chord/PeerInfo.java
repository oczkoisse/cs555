package cs555.a2.chord;

import java.math.BigInteger;
import java.net.InetSocketAddress;

public class PeerInfo
{
    private InetSocketAddress listeningAddress;
    private String name;
    private BigInteger id;

    public PeerInfo(BigInteger id, InetSocketAddress listeningAddress, String name)
    {
        this.id = id;
        this.listeningAddress = listeningAddress;
        this.name = name;
    }

    public PeerInfo(BigInteger id, InetSocketAddress listeningAddress)
    {
        this(id, listeningAddress, listeningAddress.getHostString());
    }

    public BigInteger getId()
    {
        return new BigInteger(id.toByteArray());
    }

    public String getName()
    {
        return name;
    }

    public InetSocketAddress getListeningAddress()
    {
        return new InetSocketAddress(listeningAddress.getAddress(), listeningAddress.getPort());
    }
}
