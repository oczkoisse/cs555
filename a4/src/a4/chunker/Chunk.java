package a4.chunker;
import a2.hash.Hash;
import a2.hash.Hasher;
import a2.hash.SHA1;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class Chunk implements Externalizable
{
    private static final Path saveDir = Paths.get(System.getProperty("java.io.tmpdir"));
    private final Hasher hasher = new SHA1();
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
     * For instantiating Chunk object from a chunk file stored with {@link #writeToFile()}
     * @param pathToChunkFile
     */
    public Chunk(Path pathToChunkFile) throws IOException, ClassNotFoundException, IntegrityCheckFailedException
    {
        List<Integer> failedSlices = new ArrayList<>();

        try(FileInputStream fin = new FileInputStream(pathToChunkFile.toString());
            BufferedInputStream bin = new BufferedInputStream(fin);
            ObjectInputStream oin = new ObjectInputStream(bin)) {
            this.metadata = (Metadata) oin.readObject();
            int sliceCount = oin.readInt();
            this.sliceList = new ArrayList<>();
            for (int i = 0; i < sliceCount; i++) {
                Hash h = (Hash) oin.readObject();
                Slice s = (Slice) oin.readObject();
                if (s.calculateHash(hasher).equals(h))
                    this.sliceList.add(s);
                else
                    failedSlices.add(i);
            }
        }
        if (failedSlices.size() > 0)
            throw new IntegrityCheckFailedException("Chunk integrity failed for " + failedSlices.size() + " slices", failedSlices);
    }

    private Path getStoragePath()
    {
        String chunkName = String.join("_", "chunk", metadata.getFileName().getName(-1).toString(),
                Long.toString(metadata.getSequenceNum()));
        return Paths.get(saveDir.toString(), chunkName);
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

    public void writeToFile() throws IOException
    {
        hasher.reset();
        try(FileOutputStream fout = new FileOutputStream(getStoragePath().toString());
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            ObjectOutputStream oout = new ObjectOutputStream(bout)) {
            oout.writeObject(metadata);
            oout.writeInt(sliceList.size());
            for (Slice s : sliceList) {
                oout.writeObject(s.calculateHash(hasher));
                oout.writeObject(s);
            }
        }
    }
}
