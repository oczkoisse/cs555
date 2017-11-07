package a4.nodes.server.messages;


import a4.chunker.Metadata;

import java.net.InetSocketAddress;
import java.util.List;

public class MajorHeartbeat extends Heartbeat
{
    public MajorHeartbeat()
    {
        super();
    }

    public MajorHeartbeat(InetSocketAddress listeningAddress, List<Metadata> allChunks)
    {
        super(listeningAddress, allChunks, allChunks.size());
    }

    @Override
    public ServerMessageType getMessageType()
    {
        return ServerMessageType.MAJOR_HEARTBEAT;
    }
}
