package a4.nodes.controller.messages;

import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;

public class ReadReply extends Notification<ControllerMessageType> {
    private InetSocketAddress replica;
    private boolean hasReplica;

    public ReadReply()
    {
        this.replica = null;
        this.hasReplica = false;
    }

    public ReadReply(InetSocketAddress replica)
    {
        if (replica == null)
            throw new NullPointerException("Replica is null");
        this.replica = replica;
        this.hasReplica = true;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.READ_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(hasReplica);
        if(hasReplica)
        {
            out.writeObject(replica);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.hasReplica = in.readBoolean();
        if (this.hasReplica)
        {
            this.replica = (InetSocketAddress) in.readObject();
        }
    }

    public InetSocketAddress getReplica() {
        return replica;
    }

    public boolean hasReplica() { return hasReplica; }
}
