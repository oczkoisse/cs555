package a4.hash;

import a4.util.ByteConverter;

import java.util.Date;

public interface Hasher
{
    void update(int b);
    void update(byte[] data, int offset, int len);
    void reset();
    Hash getValue();
    int size();

    default int sizeInBits()
    {
        return size() * Byte.SIZE;
    }

    default Hash randomHash()
    {
        reset();

        Date d = new Date();
        long timestamp = d.getTime();
        byte[] bytes = ByteConverter.convert(timestamp);
        update(bytes, 0, bytes.length);
        return getValue();
    }

    static Hasher getHasherByName(Hash.Name name)
    {
        switch(name)
        {
            case SHA1:
                return new SHA1();
            case CRC16:
                return new CRC16();
            default:
                return null;
        }
    }
}
