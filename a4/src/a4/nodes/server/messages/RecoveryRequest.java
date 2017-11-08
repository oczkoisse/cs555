package a4.nodes.server.messages;

import a4.chunker.Metadata;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RecoveryRequest implements Message<ServerMessageType> {

    private Metadata chunkInfo;
    private int listeningPort;

    public RecoveryRequest(Metadata chunkInfo, int listeningPort) {
        if (chunkInfo == null)
            throw new NullPointerException("Metadata is null");

        if (listeningPort < 0)
            throw new IllegalArgumentException("Listening Port must be non-negative");

        this.chunkInfo = chunkInfo;
        this.listeningPort = listeningPort;
    }

    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RECOVERY_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(chunkInfo);
        out.writeInt(listeningPort);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.chunkInfo = (Metadata) in.readObject();
        this.listeningPort = in.readInt();
    }

    public String getFilename()
    {
        return chunkInfo.getFileName().toString();
    }

    public long getSequenceNum()
    {
        return chunkInfo.getSequenceNum();
    }

    public int getListeningPort()
    {
        return listeningPort;
    }

    @Override
    public Enum isRequestFor()
    {
        return ServerMessageType.RECOVERY_REQUEST;
    }
}
