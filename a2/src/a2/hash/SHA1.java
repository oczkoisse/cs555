package a2.hash;

import a2.util.ByteConverter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 implements Hasher
{
    private MessageDigest hasher;

    public SHA1()
    {
        try {
            this.hasher = MessageDigest.getInstance("SHA-1");
        }
        catch(NoSuchAlgorithmException ex) { /* Can't happen */ }
    }

    @Override
    public void update(int b)
    {
        byte[] intBytes = ByteConverter.convert(b);
        update(intBytes, 0, intBytes.length);
    }

    @Override
    public void update(byte[] data, int offset, int len)
    {
        this.hasher.update(data, offset, len);
    }

    @Override
    public void reset()
    {
        this.hasher.reset();
    }

    @Override
    public Hash getValue()
    {
        return new Hash(this.hasher.digest());
    }

    @Override
    public int size()
    {
        return this.hasher.getDigestLength();
    }
}
