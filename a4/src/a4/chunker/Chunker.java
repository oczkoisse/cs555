package a4.chunker;

import a2.hash.CRC16;
import a2.hash.Hash;
import a2.hash.HashName;
import a2.hash.SHA1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Chunker implements Iterable<Chunk>, AutoCloseable
{
    private Size chunkSize;
    private Size sliceSize;

    private ChunkIterator it;
    private boolean closed;

    private final Hash hasher;

    public Chunker(Path fileName, Size chunkSize, Size sliceSize, HashName hashName) throws IOException
    {
        if (chunkSize.getByteCount() % sliceSize.getByteCount() != 0)
            throw new IllegalArgumentException(String.format("Chunk size %s is not a multiple of slice size %s", chunkSize, sliceSize));

        this.chunkSize = chunkSize;
        this.sliceSize = sliceSize;

        switch(hashName)
        {
            case SHA1:
                hasher = new SHA1();
                break;
            case CRC16:
                hasher = new CRC16();
                break;
            default:
                // Will never happen
                hasher = null;
                break;
        }

        this.it = new ChunkIterator(fileName);
        this.closed = false;
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

        public ChunkIterator(Path filePath) throws IOException
        {
            this.fileName = filePath.getFileName();
            this.file = new BufferedInputStream(Files.newInputStream(filePath));
            this.sliceBuffer = new byte[sliceSize.getByteCount()];
            this.seq = 0;
        }

        @Override
        public boolean hasNext()
        {
            boolean _hasNext = true;
            this.file.mark(1);
            try
            {
                if (this.file.read() == -1)
                    _hasNext = false;
            }
            catch (IOException ex)
            {
                // Read failed
                _hasNext = false;
            }
            finally
            {
                try { this.file.reset(); } catch (IOException ex) { /* Should never happen */ }
            }
            return _hasNext;
        }

        @Override
        public Chunk next()
        {
            int slicesPerChunk = chunkSize.getByteCount()/sliceSize.getByteCount();

            List<Slice> sliceList = new ArrayList<>();

            for(int i=0; i < slicesPerChunk && hasNext(); i++)
            {
                try
                {
                    int bytesRead = this.file.read(sliceBuffer);
                    sliceList.add(new Slice(sliceBuffer, sliceSize, hasher));
                }
                catch(IOException ex)
                {
                    /* Should not happen */
                }
            }

            if (sliceList.size() == 0)
                throw new NoSuchElementException("next() called on an ended stream");

            Metadata m = new Metadata(fileName.toString(), seq, 0);
            return new Chunk(m, sliceList);
        }

        private void close() throws IOException
        {
            this.file.close();
        }
    }
}
