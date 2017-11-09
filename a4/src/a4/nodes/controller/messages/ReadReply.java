package a4.nodes.controller.messages;

import a4.nodes.client.messages.ClientMessageType;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class ReadReply implements Message<ControllerMessageType> {
    private InetSocketAddress replica;
    private boolean failed;

    public ReadReply()
    {
        this.replica = null;
        this.failed = true;
    }

    public ReadReply(InetSocketAddress replica)
    {
        if (replica == null)
            throw new NullPointerException("Replica is null");
        this.replica = replica;
        this.failed = false;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.READ_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(failed);
        if(!failed)
        {
            out.writeObject(replica);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.failed = in.readBoolean();
        if (!this.failed)
        {
            this.replica = (InetSocketAddress) in.readObject();
        }
    }

    public InetSocketAddress getReplica() {
        return replica;
    }

    public boolean isFailed() { return failed; }

    @Override
    public Enum isResponseTo()
    {
        return ClientMessageType.READ_REQUEST;
    }
}
