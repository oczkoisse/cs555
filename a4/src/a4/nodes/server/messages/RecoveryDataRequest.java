package a4.nodes.server.messages;


import a4.chunker.Metadata;
import a4.transport.Message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecoveryDataRequest implements Message<ServerMessageType> {

    private Metadata chunkInfo;
    private List<Integer> failedSlices;
    private int listeningPort;

    public RecoveryDataRequest() {
        this.chunkInfo = null;
        this.failedSlices = null;
        this.listeningPort = -1;
    }

    public RecoveryDataRequest(Metadata chunkInfo, List<Integer> failedSlices, int listeningPort) {
        if (chunkInfo == null)
            throw new NullPointerException("Metadata is null");
        if (failedSlices == null || failedSlices.size() == 0)
            throw new IllegalArgumentException("Failed slice list is invalid");
        if (listeningPort < 0)
            throw new IllegalArgumentException("Listening Port must be non-negative");

        this.chunkInfo = chunkInfo;
        this.failedSlices = failedSlices;
        this.listeningPort = listeningPort;
    }


    @Override
    public ServerMessageType getMessageType() {
        return ServerMessageType.RECOVERY_DATA_REQUEST;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(chunkInfo);
        out.writeInt(failedSlices.size());
        for (Integer i : failedSlices)
            out.writeInt(i);
        out.writeInt(listeningPort);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        this.chunkInfo = (Metadata) in.readObject();
        if (failedSlices == null)
            failedSlices = new ArrayList<>();
        else
            failedSlices.clear();

        int sz = in.readInt();
        for (int i = 0; i < sz; i++)
            failedSlices.add(in.readInt());

        this.listeningPort = in.readInt();
    }

    public List<Integer> getFailedSlices() {
        return Collections.unmodifiableList(failedSlices);
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getFilename() {
        return chunkInfo.getFileName().toString();
    }

    public long getSequenceNum() {
        return chunkInfo.getSequenceNum();
    }

    @Override
    public Enum isRequestFor()
    {
        return ServerMessageType.RECOVERY_DATA;
    }
}
