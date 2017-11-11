package a4.chunker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

public class Metadata implements Externalizable
{
    private static final long serialVersionUID = 1L;

    private static final Path saveDir = Paths.get(System.getProperty("java.io.tmpdir"));

    private static final Calendar calendar = Calendar.getInstance();
    private Path fileName;
    private long sequenceNum;
    private int version;
    private Date timestamp;

    public Metadata(String file, long sequenceNum, int version) throws InvalidPathException
    {
        this(file, sequenceNum, version, calendar.getTime());
    }

    public Metadata(String file, long sequenceNum, int version, Date timestamp) throws InvalidPathException
    {
        this.fileName = Paths.get(file).getFileName();
        this.sequenceNum = sequenceNum;
        this.version = version;
        this.timestamp = timestamp;
    }

    public Metadata()
    {
        this.fileName = null;
        this.sequenceNum = 0;
        this.version = 0;
        this.timestamp = null;
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


    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeUTF(fileName.toString());
        out.writeLong(sequenceNum);
        out.writeInt(version);
        out.writeObject(timestamp);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.fileName = Paths.get(in.readUTF());
        this.sequenceNum = in.readLong();
        this.version = in.readInt();
        this.timestamp = (Date) in.readObject();
    }

    public Path getStoragePath()
    {
        String chunkName = String.join("_", "chunk", getFileName().toString(),
                Long.toString(getSequenceNum()));

        return Paths.get(saveDir.toString(), chunkName);
    }

    @Override
    public String toString()
    {
        return fileName + ", " + sequenceNum + ", " + version + ", " + timestamp;
    }

}
