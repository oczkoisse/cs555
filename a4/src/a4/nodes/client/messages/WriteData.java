package a4.nodes.client.messages;

import a4.chunker.Chunk;
import a4.nodes.controller.messages.WriteReply;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class WriteData implements Message<ClientMessageType> {

    private Chunk chunk;
    private List<InetSocketAddress> forwardingAddresses;

    public WriteData()
    {
        this.chunk = null;
        this.forwardingAddresses = null;
    }

    public WriteData(Chunk chunk, WriteReply writeReply)
    {
        if (chunk == null)
            throw new NullPointerException("Chunk data is null");
        if (writeReply == null)
            throw new NullPointerException("Write Reply message is null");

        this.chunk = chunk;
        this.forwardingAddresses = new ArrayList<>(writeReply.getNodesToWriteTo());
    }

    @Override
    public ClientMessageType getMessageType() {
        return ClientMessageType.WRITE_DATA;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(forwardingAddresses.size());
        for(InetSocketAddress addr: forwardingAddresses)
            out.writeObject(addr);

        out.writeObject(chunk);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        if (forwardingAddresses != null)
            forwardingAddresses.clear();

        int sz = in.readInt();
        if (sz > 1)
            forwardingAddresses = new ArrayList<>();
        for(int i=0; i<sz; i++) {
            InetSocketAddress addr = (InetSocketAddress) in.readObject();
            if (i > 0)
                forwardingAddresses.add(addr);
        }
        this.chunk = (Chunk) in.readObject();
    }

    public InetSocketAddress getForwardingAddress()
    {
        if (forwardingAddresses != null && forwardingAddresses.size() > 0)
            return forwardingAddresses.get(0);
        return null;
    }

    public Chunk getChunk()
    {
        return chunk;
    }
}
