package a4.chunker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Chunker implements Iterable<Chunk>, AutoCloseable
{
    private Size chunkSize;
    private Size sliceSize;

    private ChunkIterator it;
    private boolean closed;

    public Chunker(Path fileName, Size chunkSize, Size sliceSize) throws IOException
    {
        if (chunkSize.getByteCount() % sliceSize.getByteCount() != 0)
            throw new IllegalArgumentException(String.format("Chunk size %s is not a multiple of slice size %s", chunkSize, sliceSize));

        this.chunkSize = chunkSize;
        this.sliceSize = sliceSize;

        this.it = new ChunkIterator(fileName);
        this.closed = false;
    }

    public static void combineChunksToFile(List<Chunk> chunks, Path outDir) throws IOException
    {
        if(chunks == null)
            throw new NullPointerException("Chunk list is null");
        else if (chunks.size() == 0)
            throw new IllegalArgumentException("Chunk list has size 0");
        else if (chunks.contains(null))
            throw new NullPointerException("Chunk list has null elements");
        else if (outDir == null)
            throw new NullPointerException("Path is null");

        // Sort chunks by sequence number
        chunks.sort((Chunk c1, Chunk c2) -> Long.compare(c1.getMetadata().getSequenceNum(), c2.getMetadata().getSequenceNum()));

        Path outFile = Paths.get(outDir.toString(), chunks.get(0).getMetadata().getFileName().toString());

        boolean success = true;
        try(FileOutputStream fout = new FileOutputStream(outFile.toString());
            BufferedOutputStream bout = new BufferedOutputStream(fout))
        {
            long expectedSeqNum = 0;
            for(Chunk c: chunks)
            {
                long readSequenceNum = c.getMetadata().getSequenceNum();
                if(readSequenceNum == expectedSeqNum)
                    expectedSeqNum++;
                else
                {
                    success = false;
                    throw new IllegalArgumentException(String.format("Mismatch in chunk sequence number: got %d, expected %d", readSequenceNum, expectedSeqNum));
                }
                for(Slice s: c)
                {
                    byte[] sliceData = s.getSliceData();
                    bout.write(sliceData, 0, sliceData.length);
                }
            }
        }
        finally
        {
            if(!success && Files.exists(outFile))
            {
                Files.delete(outFile);
            }
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
                    byte[] sliceData = bytesRead < sliceSize.getByteCount() ?
                            Arrays.copyOfRange(sliceBuffer, 0, bytesRead) : sliceBuffer;

                    sliceList.add(new Slice(sliceData, sliceSize));
                }
                catch(IOException ex)
                {
                    /* Should not happen */
                }
            }

            if (sliceList.size() == 0)
                throw new NoSuchElementException("next() called on an ended stream");

            Metadata m = new Metadata(fileName.toString(), seq++, 0);
            return new Chunk(m, sliceList);
        }

        private void close() throws IOException
        {
            this.file.close();
        }
    }
}
