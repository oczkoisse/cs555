package a4.chunker;

import java.util.ArrayList;
import java.util.List;

public class IntegrityCheckFailedException extends Exception {

    private List<Integer> failedSlices;

    private void setFailedSlices(List<Integer> failedSlices)
    {
        if (failedSlices == null)
            throw new NullPointerException("List of failed slices cannot be null");
        if (failedSlices.size() == 0)
            throw new IllegalArgumentException("List of failed slices must have size greater than 0");

        this.failedSlices = new ArrayList<>(failedSlices);
    }

    public IntegrityCheckFailedException(String message, List<Integer> failedSlices)
    {
        super(message);
        setFailedSlices(failedSlices);
    }

    public IntegrityCheckFailedException(Throwable cause, List<Integer> failedSlices)
    {
        super(cause);
        setFailedSlices(failedSlices);
    }

    public IntegrityCheckFailedException(String message, Throwable cause, List<Integer> failedSlices)
    {
        super(message, cause);
        setFailedSlices(failedSlices);
    }
}
