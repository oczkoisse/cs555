package a4.chunker;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChunkerTest
{
    @Test
    public void combineChunksToFile() throws Exception
    {
        String testFile = getClass().getResource("/child_of_light.jpg").getPath().substring(1);
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
                reReadChunks.add(new Chunk(c.getStoragePath()));
        }

        System.out.println("Writing combined file to " + Paths.get(testFile).getParent().getParent());
        Chunker.combineChunksToFile(reReadChunks, Paths.get(testFile).getParent().getParent());

    }

}