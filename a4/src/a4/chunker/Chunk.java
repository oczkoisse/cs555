package a4.chunker;
import a2.hash.Hash;
import a2.hash.SHA1;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class Chunk implements Externalizable
{
    private static final Path saveDir = Paths.get(System.getProperty("java.io.tmpdir"));
    private Hash hasher = new SHA1();
    private Metadata metadata;
    private List<Slice> sliceList;

    public Chunk(Metadata metadata, List<Slice> sliceList)
    {
        if (metadata == null)
            throw new NullPointerException("Metadata is null");
        if (sliceList == null)
            throw new NullPointerException("Slice list is null");
        if (sliceList.size() == 0)
            throw new IllegalArgumentException("Slice list size should be greater than 0");

        this.metadata = metadata;
        this.sliceList = sliceList;
    }

    /**
     * For reading actual chunk contents based on the in memory metadata
     * @param metadata
     */
    public Chunk(Metadata metadata)
    {
        Paths.get(saveDir.toString(), metadata.getFileName().toString())
        this.metadata = metadata;

    }

    public Chunk()
    {
        this.metadata = null;
        this.sliceList = null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(metadata);
        out.writeInt(sliceList.size());
        for(Slice s: sliceList)
            out.writeObject(s);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.sliceList = new ArrayList<>();
        int sliceCount = in.readInt();
        for (int i=0; i < sliceCount; i++)
            sliceList.add((Slice) in.readObject());
    }

    public void writeToFile()
    {

    }
}
