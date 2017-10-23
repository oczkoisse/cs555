package a4.chunker;

import a2.hash.CRC16;
import a2.hash.Hash;
import a2.hash.HashName;
import a2.hash.SHA1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class Chunker implements Iterable<Chunk>, AutoCloseable
{
    private Size chunkSize;
    private Size sliceSize;

    private ChunkIterator it;
    private boolean closed;

    private Hash hasher;

    public Chunker(Path fileName, Size chunkSize, Size sliceSize, HashName hashName) throws IOException
    {
        if (chunkSize.getByteCount() % sliceSize.getByteCount() != 0)
            throw new IllegalArgumentException(String.format("Chunk size %s is not a multiple of slice size %s", chunkSize, sliceSize));

        this.chunkSize = chunkSize;
        this.sliceSize = sliceSize;

        this.it = new ChunkIterator(fileName);
        this.closed = false;

        switch(hashName)
        {
            case CRC16:
                this.hasher = new CRC16();
                break;
            case SHA1:
                this.hasher = new SHA1();
                break;
        }
    }

    @Override
    public Iterator<Chunk> iterator()
    {
        return it;
    }

    @Override
    public void close() throws IOException
    {
        if (!closed && it != null)
            this.it.close();
        closed = true;
    }

    private class ChunkIterator implements Iterator<Chunk>
    {
        private final Path fileName;
        private final BufferedInputStream file;

        private final byte[] sliceBuffer;
        private int seq;

        public ChunkIterator(Path fileName) throws IOException
        {
            this.fileName = fileName;

            this.file = new BufferedInputStream(Files.newInputStream(fileName));
            this.sliceBuffer = new byte[sliceSize.getByteCount()];

            this.seq = 0;
        }

        @Override
        public boolean hasNext()
        {
            return true;
        }

        @Override
        public Chunk next()
        {
            int bytesRead;
            try
            {
                bytesRead = this.file.read(buffer);
            }
            catch(IOException ex)
            {

            }
            Metadata m = new Metadata(fileName.toString(), seq, 0);
            return new Chunk(m);
        }

        private void close() throws IOException
        {
            this.file.close();
        }
    }
}
