package a4.chunker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegrityCheckFailedException extends Exception {

    private List<Integer> failedSlices;
    private Chunk chunk;

    private void setFailedSlices(List<Integer> failedSlices)
    {
        if (failedSlices == null)
            throw new NullPointerException("List of failed slices cannot be null");
        if (failedSlices.size() == 0)
            throw new IllegalArgumentException("List of failed slices must have size greater than 0");

        this.failedSlices = new ArrayList<>(failedSlices);
    }

    private void setChunk(Chunk c)
    {
        if (c == null)
            throw new NullPointerException("Metadata is null");

        this.chunk = c;
    }

    public IntegrityCheckFailedException(String message, List<Integer> failedSlices, Chunk chunk)
    {
        super(message);
        setFailedSlices(failedSlices);
        setChunk(chunk);
    }

    public IntegrityCheckFailedException(Throwable cause, List<Integer> failedSlices, Chunk chunk)
    {
        super(cause);
        setFailedSlices(failedSlices);
        setChunk(chunk);
    }

    public IntegrityCheckFailedException(String message, Throwable cause, List<Integer> failedSlices, Metadata chunkInfo)
    {
        super(message, cause);
        setFailedSlices(failedSlices);
        setChunk(chunk);
    }

    public List<Integer> getFailedSlices()
    {
        return Collections.unmodifiableList(failedSlices);
    }

    public Chunk getChunk()
    {
        return chunk;
    }
}
