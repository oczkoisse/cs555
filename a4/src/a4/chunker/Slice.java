package a4.chunker;

import a2.hash.Hash;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

class Slice implements Externalizable
{
    private Size sliceSize;
    private byte[] hashValue;
    private byte[] sliceData;

    // Meant only for writing to DataOutput
    public Slice()
    {
        this.sliceSize = null;
        this.hashValue = null;
        this.sliceData = null;
    }

    public Slice(byte[] sliceData, Size sliceSize, Hash hasher)
    {
        if (sliceData.length == 0 || sliceData.length > sliceSize.getByteCount())
            throw new IllegalArgumentException("Slice data length " + sliceData.length + " is invalid for a slice of size " + sliceSize);

        this.sliceData = Arrays.copyOf(sliceData, sliceData.length);

        hasher.reset();
        hasher.update(sliceData, 0, sliceData.length);
        this.hashValue = hasher.getValue();

        this.sliceSize = sliceSize;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(hashValue.length);
        out.write(hashValue);

        out.writeObject(sliceSize);
        out.write(sliceData);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.hashValue = new byte[in.readInt()];
        in.read(this.hashValue);

        this.sliceSize = (Size) in.readObject();
        this.sliceData = new byte[this.sliceSize.getByteCount()];
        in.read(this.sliceData);
    }
}