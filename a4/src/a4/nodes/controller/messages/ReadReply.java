package a4.nodes.controller.messages;

import a4.nodes.client.messages.ClientMessageType;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class ReadReply implements Message<ControllerMessageType> {
    private InetSocketAddress replica;
    private boolean done;
    private boolean failed;

    public ReadReply()
    {
        this.failed = true;
        this.replica = null;
        this.done = true;
    }

    public ReadReply(InetSocketAddress replica)
    {
        this.failed = false;
        if (replica == null)
            throw new NullPointerException("Replica is null");
        this.replica = replica;
        this.done = false;
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
            out.writeBoolean(done);
            if(!done)
                out.writeObject(replica);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.failed = in.readBoolean();
        if (!this.failed)
        {
            this.done = in.readBoolean();
            if (!this.done)
                this.replica = (InetSocketAddress) in.readObject();
            else
                this.replica = null;
        }
    }

    public InetSocketAddress getReplica() {
        return replica;
    }

    public boolean isDone()
    {
        return done;
    }

    public boolean isFailed() { return failed; }

    @Override
    public Enum isResponseTo()
    {
        return ClientMessageType.READ_REQUEST;
    }
}
