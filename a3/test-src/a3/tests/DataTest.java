package a3.tests;

import a3.data.Data;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataTest
{
    @Test
    void parseTSV()
    {
        try (FileInputStream fin = new FileInputStream(getClass().getResource("/null.tsv").getPath());
             InputStreamReader ins = new InputStreamReader(fin);
             BufferedReader buf = new BufferedReader(ins))
        {
            String line;
            while((line = buf.readLine()) != null)
            {
                Data d = new Data(line);
                if (d.isValid()) {
                    String id = d.getEchoNestId();
                    String artistName = d.getArtistName();
                    List<String> artistTerms = d.getArtistTerms();
                    List<Float> artistTermsFreq = d.getArtistTermsFreq();
                    Map<String, Float> x = d.getArtistTermsWithFreq();
                    List<String> popularArtistTerms = d.getPopularArtistTerms();
                    //System.out.println(artistName);
                    System.out.println(artistTerms);
                    System.out.println(artistTermsFreq);
                    System.out.println(x);
                    System.out.println(popularArtistTerms);
                    System.out.println("------");
                    Float danceability = d.getDanceability();;
                    Float loudness = d.getLoudness();
                    Float songHotness = d.getSongHotness();
                    Float tempo = d.getTempo();

                    assertNotNull(id);
                    assertNotNull(artistName);
                    assertNotNull(artistTerms);
                    assertNotNull(danceability);
                    assertNotNull(loudness);
                    assertNotNull(tempo);

                    //System.out.println(id);
                    //System.out.println(artistName);
                    //System.out.println(artistTerms);
                    //System.out.println(danceability);
                    //System.out.println(loudness);
                    //System.out.println(songHotness);
                    //System.out.println(tempo);
                }
            }
        }
        catch(IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }
}