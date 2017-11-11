package a4.chunker;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChunkerTest
{
    @Test
    public void createWriteAndReadChunks() throws Exception
    {
        String testFile = getClass().getResource("/big_text.txt").getPath();
        System.out.println("Chunking " + testFile);
        Size chunkSize = new Size(64, Size.Unit.K);
        Size sliceSize = new Size(8, Size.Unit.K);

        List<Chunk> chunks = new ArrayList<>();
        List<Chunk> reReadChunks = new ArrayList<>();
        try(Chunker chunker = new Chunker(Paths.get(testFile), chunkSize, sliceSize))
        {
            for(Chunk c: chunker)
            {
                chunks.add(c);
            }
            Assert.assertTrue(chunks.size() != 0);


            for(Chunk c: chunks)
                c.writeToFile();

            for(Chunk c: chunks)
                reReadChunks.add(new Chunk(c.getMetadata().getStoragePath()));
        }

        System.out.println("Writing combined file to " + Paths.get(testFile).getParent().getParent());
        Chunker.combineChunksToFile(reReadChunks, Paths.get(testFile).getParent().getParent());
    }

    @Test
    public void testCreateChunk() throws  IOException
    {
        Size chunkSize = new Size(64, Size.Unit.K);
        Size sliceSize = new Size(8, Size.Unit.K);

        Path source = Paths.get(getClass().getResource("/big_text.txt").getPath());
        try(Chunker chunker = new Chunker(source, chunkSize, sliceSize))
        {
            for(Chunk c: chunker)
            {
                c.writeToFile();
            }
        }
    }

    @Test
    public void testMessedChunk() throws IOException
    {
        Path chunkMessedAtBeginning = Paths.get(getClass().getResource("/chunk_start.txt_6").getPath());
        Path chunkMessedInBetween = Paths.get(getClass().getResource("/chunk_between.txt_6").getPath());
        Path chunkMessedAtEnd = Paths.get(getClass().getResource("/chunk_end.txt_6").getPath());

        Size chunkSize = new Size(64, Size.Unit.K);
        Size sliceSize = new Size(8, Size.Unit.K);

        List<Path> paths = new ArrayList<>();

        paths.add(chunkMessedAtBeginning);
        paths.add(chunkMessedInBetween);
        paths.add(chunkMessedAtEnd);

        for(Path p : paths)
        {
            try
            {
                Chunk c = new Chunk(p);
                Assert.fail(p.getFileName() + " should have failed integrity check");
            }
            catch(IntegrityCheckFailedException ex)
            {
                System.out.println(ex.getMessage());
            }
            catch(IOException ex)
            {
                System.out.println(ex + ", " + ex.getMessage());
            }
        }

    }

}