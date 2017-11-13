package a4.nodes.server.messages;


import a4.chunker.Metadata;
import a4.transport.Request;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecoveryDataRequest extends Request<ServerMessageType> {

    private Metadata chunkInfo;
    private List<Integer> failedSlices;

    public RecoveryDataRequest() {
        this.chunkInfo = null;
        this.failedSlices = null;
    }

    public RecoveryDataRequest(Metadata chunkInfo, List<Integer> failedSlices) {
        if (chunkInfo == null)
            throw new NullPointerException("Metadata is null");
        if (failedSlices == null || failedSlices.size() == 0)
            throw new IllegalArgumentException("Failed slice list is invalid");

        this.chunkInfo = chunkInfo;
        this.failedSlices = failedSlices;
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
    }

    public List<Integer> getFailedSlices() {
        return Collections.unmodifiableList(failedSlices);
    }

    public String getFilename() {
        return chunkInfo.getFileName().toString();
    }

    public long getSequenceNum() {
        return chunkInfo.getSequenceNum();
    }
}
