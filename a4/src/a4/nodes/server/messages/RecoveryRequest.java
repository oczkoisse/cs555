package a4.nodes.server.messages;

import a4.chunker.Metadata;
import a4.transport.Message;
import a4.transport.Request;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RecoveryRequest extends Request<ServerMessageType> {

    private Metadata chunkInfo;

    public RecoveryRequest(Metadata chunkInfo) {
        if (chunkInfo == null)
            throw new NullPointerException("Metadata is null");

        this.chunkInfo = chunkInfo;
    }

    public RecoveryRequest()
    {
        this.chunkInfo = null;
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RECOVERY_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(chunkInfo);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.chunkInfo = (Metadata) in.readObject();
    }

    public String getFilename()
    {
        return chunkInfo.getFileName().toString();
    }

    public long getSequenceNum()
    {
        return chunkInfo.getSequenceNum();
    }
}
