package a4.chunker;
import a4.hash.Hash;
import a4.hash.Hasher;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Chunk implements Externalizable, Iterable<Slice>
{
    private final Hasher hasher = Hasher.getHasherByName(Hash.Name.SHA1);
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
    public Chunk(Path pathToChunkFile) throws IOException, IntegrityCheckFailedException
    {
        List<Integer> failedSlices = new ArrayList<>();
        StringBuilder exMessage = new StringBuilder();
        exMessage.append(String.format("%n"));

        int curSlice = 0, sliceCount = 0;

        try(FileInputStream fin = new FileInputStream(pathToChunkFile.toString());
            BufferedInputStream bin = new BufferedInputStream(fin);
            ObjectInputStream oin = new ObjectInputStream(bin)) {

            this.metadata = (Metadata) oin.readObject();
            sliceCount = oin.readInt();

            this.sliceList = new ArrayList<>();
            for (; curSlice < sliceCount; curSlice++) {
                Hash expectedHash = (Hash) oin.readObject();
                Slice s = (Slice) oin.readObject();

                Hash calculatedHash = s.calculateHash(hasher);
                if (!expectedHash.equals(calculatedHash))
                {
                    exMessage.append(String.format("Hash check failed for slice %d: expected %s, got %s.%n", curSlice, expectedHash, calculatedHash));
                    failedSlices.add(curSlice);
                }
                else
                    this.sliceList.add(s);
            }
        }
        catch(StreamCorruptedException | ClassNotFoundException ex)
        {
            if (curSlice == 0 && sliceCount == 0)
                throw new IOException("Metadata/slice count info is corrupted");

            // Type error could occur while reading an object if metadata or any slice is corrupted
            while(curSlice < sliceCount)
            {
                failedSlices.add(curSlice);
                curSlice++;
            }
        }

        if (failedSlices.size() > 0)
        {
            exMessage.append(String.format("Integrity check failed in %s for %d slices.%n", pathToChunkFile, failedSlices.size()));
            throw new IntegrityCheckFailedException(exMessage.toString(), failedSlices);
        }
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
        try(FileOutputStream fout = new FileOutputStream(this.metadata.getStoragePath().toString());
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

    public Metadata getMetadata()
    {
        return this.metadata;
    }

    @Override
    public Iterator<Slice> iterator()
    {
        return sliceList.iterator();
    }
}
