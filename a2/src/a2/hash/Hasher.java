package a2.hash;

import java.util.Date;
import a2.util.ByteConverter;

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
}
