package a3.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Data
{
    // Check against header
    private boolean header;

    // Fields
    private final String echoNestId;
    private final String artistName;
    private final List<String> artistTerms;
    private final Float danceability;
    private final Float loudness;
    private final Float songHotness;
    private final Float tempo;

    // Field indexes
    public static final int ECHO_NEST_ID = 4,
                       ARTIST_NAME = 11,
                       ARTIST_TERMS = 13,
                       DANCEABILITY = 21,
                       LOUDNESS = 27,
                       SONG_HOTNESS = 43,
                       TEMPO = 47;

    public Data(String line)
    {
        String[] fields = line.split("\\t");
        for(int i = 0; i<fields.length; i++)
            fields[i] = fields[i].trim();

        if(fields[0].equalsIgnoreCase("analysis_sample_rate")) {
            header = true;
            echoNestId = null;
            artistName = null;
            artistTerms = null;
            danceability = null;
            loudness = null;
            songHotness = null;
            tempo = null;
        }
        else
        {
            header = false;
            echoNestId = parseString(fields[ECHO_NEST_ID]);
            artistName = parseString(fields[ARTIST_NAME]);
            artistTerms = parseStringArray(fields[ARTIST_TERMS]);
            danceability = parseFloat(fields[DANCEABILITY]);
            loudness = parseFloat(fields[LOUDNESS]);
            songHotness = parseFloat(fields[SONG_HOTNESS]);
            tempo = parseFloat(fields[TEMPO]);
        }
    }

    private static Integer parseInt(String s)
    {
        s = s.trim();
        try {
            return Integer.parseInt(s);
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }

    private static Float parseFloat(String s)
    {
        s = s.trim();
        try {
            return Float.parseFloat(s);
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }

    private static String parseString(String s)
    {
        s = s.trim();
        if (s.equals("nan"))
            return null;
        return s;
    }


    private static boolean isStringArray(String s)
    {
        s = s.trim();
        return s.startsWith("\"[\"") && s.endsWith("\"]\"");
    }

    private static List<String> parseStringArray(String s)
    {
        List<String> parsed = new ArrayList<>();
        String[] elements = s.split("\"\"");
        for(int i = 1; i < elements.length; i+=2)
        {
            parsed.add(elements[i].trim());
        }
        return Collections.unmodifiableList(parsed);
    }

    private static boolean isNumericArray(String s)
    {
        s = s.trim();
        return s.startsWith("[") && s.endsWith("]");
    }

    private static List<Integer> parseIntArray(String s)
    {
        s = s.replace("[\\[\\]]", "");

        List<Integer> parsed = new ArrayList<>();
        String[] elements = s.split(",");

        try{
            for (String e: elements)
            {
                parsed.add(Integer.parseInt(e.trim()));
            }
        }
        catch(NumberFormatException ex)
        {
            elements = null;
        }
        return Collections.unmodifiableList(parsed);
    }

    private static List<Float> parseFloatArray(String s)
    {
        s = s.replace("[\\[\\]]", "");

        List<Float> parsed = new ArrayList<>();
        String[] elements = s.split(",");

        try{
            for (String e: elements)
            {
                parsed.add(Float.parseFloat(e.trim()));
            }
        }
        catch(NumberFormatException ex)
        {
            elements = null;
        }
        return Collections.unmodifiableList(parsed);
    }

    public boolean isHeader()
    {
        return header;
    }

    public String getEchoNestId()
    {
        return echoNestId;
    }

    public String getArtistName()
    {
        return artistName;
    }

    public List<String> getArtistTerms()
    {
        return artistTerms;
    }

    public Float getDanceability()
    {
        return danceability;
    }

    public Float getLoudness()
    {
        return loudness;
    }

    public Float getSongHotness()
    {
        return songHotness;
    }

    public Float getTempo()
    {
        return tempo;
    }
}
