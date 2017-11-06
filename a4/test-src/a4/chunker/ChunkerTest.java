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
        String testFile = getClass().getResource("/big_text.txt").getPath().substring(1);
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
    public void testMessedChunk()
    {
        Path chunkMessedAtBeginning = Paths.get(getClass().getResource("/chunk_messed_at_beginning.txt_0").getPath().substring(1));
        Path chunkMessedAtEnd = Paths.get(getClass().getResource("/chunk_messed_at_end.txt_0").getPath().substring(1));
        Path chunkMessedInBetween = Paths.get(getClass().getResource("/chunk_messed_in_between.txt_0").getPath().substring(1));

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
            catch(IntegrityCheckFailedException | IOException ex)
            {
                System.out.println(ex.getMessage());
            }
        }

    }

}