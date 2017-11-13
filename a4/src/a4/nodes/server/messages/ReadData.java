package a4.nodes.server.messages;


import a4.chunker.Chunk;
import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ReadData extends Notification<ServerMessageType> {

    private Chunk chunk;

    public ReadData(Chunk chunk)
    {
        if (chunk == null)
            throw new NullPointerException("Chunk data is null");
        this.chunk = chunk;
    }

    public ReadData()
    {
        this.chunk = null;
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.READ_DATA;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(chunk);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.chunk = (Chunk) in.readObject();
    }

    public Chunk getChunk()
    {
        return chunk;
    }
}
