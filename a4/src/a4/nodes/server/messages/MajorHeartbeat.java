package a4.nodes.server.messages;


import a4.chunker.Metadata;

import java.net.InetSocketAddress;
import java.util.List;

public class MajorHeartbeat extends Heartbeat
{
    private InetSocketAddress listeningAddress;

    public MajorHeartbeat()
    {
        super();
        listeningAddress = null;
    }

    public MajorHeartbeat(List<Metadata> allChunks, InetSocketAddress listeningAddress)
    {
        super(allChunks, allChunks.size());

        if (listeningAddress == null)
            throw new NullPointerException("null listening address passed");

        this.listeningAddress = listeningAddress;
    }

    @Override
    public ServerMessageType getMessageType()
    {
        return ServerMessageType.MAJOR_HEARTBEAT;
    }

    public InetSocketAddress getListeningAddress()
    {
        return listeningAddress;
    }
}
