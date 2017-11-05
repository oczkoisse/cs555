package a2.hash;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.util.Arrays;

public class Hash implements Externalizable {

    private byte[] value;

    public enum Name
    {
        SHA1, CRC16;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hash hash = (Hash) o;

        return Arrays.equals(value, hash.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    public Hash(byte[] value)
    {
        if (value == null)
            throw new NullPointerException("null value passed to Hash");
        if (value.length == 0)
            throw new IllegalArgumentException("Hash value cannot be 0 bytes");
        this.value = Arrays.copyOf(value, value.length);
    }

    public Hash()
    {
        this.value = null;
    }

    public int size()
    {
        return value.length;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.value.length);
        out.write(this.value);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int hashLength = in.readInt();
        this.value = new byte[hashLength];
        in.read(this.value);
    }

    public BigInteger asBigInteger()
    {
        return new BigInteger(value);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for(byte b: value)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
