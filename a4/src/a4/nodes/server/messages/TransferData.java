package a4.nodes.server.messages;


import a4.chunker.Chunk;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TransferData implements Message<ServerMessageType> {

    private Chunk chunk;

    public TransferData()
    {
        this.chunk = null;
    }

    public TransferData(Chunk chunk)
    {
        if (chunk == null)
            throw new NullPointerException("Chunk is null");
        this.chunk = chunk;
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.TRANSFER_DATA;
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
