package a4.nodes.server.messages;


import a4.chunker.Metadata;

import java.util.List;

public class MinorHeartbeat extends Heartbeat
{

    public MinorHeartbeat()
    {
        super();
    }

    public MinorHeartbeat(List<Metadata> newChunks, int totalChunks)
    {
        super(newChunks, totalChunks);
    }

    @Override
    public ServerMessageType getMessageType()
    {
        return ServerMessageType.MINOR_HEARTBEAT;
    }
}
