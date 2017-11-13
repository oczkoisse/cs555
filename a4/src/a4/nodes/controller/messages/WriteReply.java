package a4.nodes.controller.messages;

import a4.transport.Notification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WriteReply extends Notification<ControllerMessageType> {

    private List<InetSocketAddress> nodesToWriteTo;

    public WriteReply()
    {
        this.nodesToWriteTo = null;
    }

    public WriteReply(List<InetSocketAddress> nodesToWriteTo)
    {
        if (nodesToWriteTo == null)
            throw new NullPointerException("Nodes list is null");
        if (nodesToWriteTo.size() == 0)
            throw new IllegalArgumentException("Nodes list has size 0");

        this.nodesToWriteTo = nodesToWriteTo;
    }

    @Override
    public ControllerMessageType getMessageType() {
        return ControllerMessageType.WRITE_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(nodesToWriteTo.size());
        for(InetSocketAddress addr: nodesToWriteTo)
            out.writeObject(addr);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (nodesToWriteTo == null)
            nodesToWriteTo = new ArrayList<>();
        else
            nodesToWriteTo.clear();

        int sz = in.readInt();
        for(int i=0; i<sz; i++)
            nodesToWriteTo.add((InetSocketAddress) in.readObject());
    }

    public List<InetSocketAddress> getNodesToWriteTo()
    {
        return Collections.unmodifiableList(nodesToWriteTo);
    }
}
