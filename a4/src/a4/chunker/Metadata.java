package a4.chunker;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

class Metadata
{
    private static final Calendar calendar = Calendar.getInstance();
    private final Path fileName;
    private final long sequenceNum;
    private int version;
    private Date timestamp;

    public Metadata(String file, long sequenceNum, int version) throws InvalidPathException
    {
        this.fileName = Paths.get(file).getFileName();
        this.sequenceNum = sequenceNum;
        this.version = version;
        updateTimestamp();
    }

    void updateTimestamp()
    {
        synchronized (calendar)
        {
            this.timestamp = calendar.getTime();
        }
    }

    void incrementVersion()
    {
        this.version++;
    }

    public Path getFileName()
    {
        return fileName;
    }

    public long getSequenceNum()
    {
        return sequenceNum;
    }

    public int getVersion()
    {
        return version;
    }

    public long getTimestamp()
    {
        return timestamp.getTime();
    }


}
