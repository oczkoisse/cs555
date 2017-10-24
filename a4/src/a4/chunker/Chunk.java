package a4.chunker;


import java.util.Collection;
import java.util.Collections;
import java.util.List;

class Chunk
{
    private Metadata metadata;

    public Chunk(Metadata metadata, List<Slice> sliceList)
    {
        this.metadata = metadata;
        if (sliceList.size() == 0)
            throw new IllegalArgumentException("Slice list size should be greater than 0");;

    }


}
