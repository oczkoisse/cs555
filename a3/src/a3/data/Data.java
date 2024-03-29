package a3.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Data
{
    // Check against validity
    private boolean valid = false;

    // Fields
    private final String echoNestId;
    private final String artistName;
    private final List<String> artistTerms;
    private final List<Float> artistTermsFreq;
    private final Float danceability;
    private final Float loudness;
    private final Float songHotness;
    private final Float tempo;

    private final String title;
    private final Integer year;

    // Field indexes
    public static final int ECHO_NEST_ID = 4,
                       ARTIST_NAME = 11,
                       ARTIST_TERMS = 13,
                       ARTIST_TERMS_FREQ = 14,
                       DANCEABILITY = 21,
                       LOUDNESS = 27,
                       SONG_HOTNESS = 42,
                       TEMPO = 47,
                       TITLE = 50,
                       YEAR = 53;



    public Data(String line)
    {
        String[] fields = line.split("\\t");
        if (fields.length == 54 && !fields[0].equalsIgnoreCase("analysis_sample_rate"))
        {
            valid = true;
            for(int i = 0; i<fields.length; i++)
                fields[i] = fields[i].trim();

            echoNestId = parseString(fields[ECHO_NEST_ID]);
            artistName = parseString(fields[ARTIST_NAME]);
            artistTerms = parseStringArray(fields[ARTIST_TERMS]);
            artistTermsFreq = parseFloatArray(fields[ARTIST_TERMS_FREQ]);
            danceability = parseFloat(fields[DANCEABILITY]);
            loudness = parseFloat(fields[LOUDNESS]);
            songHotness = parseFloat(fields[SONG_HOTNESS]);
            tempo = parseFloat(fields[TEMPO]);
            title = parseString(fields[TITLE]);
            year = parseInt(fields[YEAR]);
        }
        else
        {
            valid = false;
            echoNestId = null;
            artistName = null;
            artistTerms = null;
            artistTermsFreq = null;
            danceability = null;
            loudness = null;
            songHotness = null;
            tempo = null;
            title = null;
            year = null;
        }
    }

    public static Integer parseInt(String s)
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

    public static Float parseFloat(String s)
    {
        s = s.trim();
        if (s.equalsIgnoreCase("nan"))
            return null;

        try {
            return Float.parseFloat(s);

        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }

    public static String parseString(String s)
    {
        return s.trim();
    }


    public static boolean isStringArray(String s)
    {
        s = s.trim();
        return s.startsWith("\"[\"") && s.endsWith("\"]\"");
    }

    public static List<String> parseStringArray(String s)
    {
        List<String> parsed = new ArrayList<>();
        String[] elements = s.split("\"\"");
        for(int i = 1; i < elements.length; i+=2)
        {
            parsed.add(parseString(elements[i]));
        }
        return parsed;
    }

    public static boolean isNumericArray(String s)
    {
        s = s.trim();
        return s.startsWith("[") && s.endsWith("]");
    }

    public static List<Integer> parseIntArray(String s)
    {
        s = s.replaceAll("[\\[\\]]", "").trim();
        List<Integer> parsed = new ArrayList<>();

        if (s.equals(""))
            return parsed;


        try{
            for (String e: s.split(","))
            {
                parsed.add(Integer.parseInt(e.trim()));
            }
        }
        catch(NumberFormatException ex)
        {
            parsed = null;
        }
        return parsed;
    }

    public static List<Float> parseFloatArray(String s)
    {
        s = s.replaceAll("[\\[\\]]", "").trim();
        List<Float> parsed = new ArrayList<>();

        if (s.equals(""))
            return parsed;

        try{
            for (String e: s.split(","))
            {
                parsed.add(Float.parseFloat(e.trim()));
            }
        }
        catch(NumberFormatException ex)
        {
            parsed = null;
        }
        return parsed;
    }

    public boolean isValid()
    {
        return valid;
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

    public List<Float> getArtistTermsFreq()
    {
        return artistTermsFreq;
    }

    public Map<String, Float> getArtistTermsWithFreq()
    {
        if (artistTerms != null && artistTermsFreq != null)
        {
            assert artistTerms.size() == artistTermsFreq.size();
            Map<String, Float> artistTermsWithFreqMap = new HashMap<>();
            for(int i=0; i<artistTerms.size(); i++)
            {
                artistTermsWithFreqMap.put(artistTerms.get(i), artistTermsFreq.get(i));
            }
            return artistTermsWithFreqMap.size() > 0 ? artistTermsWithFreqMap: null;
        }
        return null;
    }

    public List<String> getPopularArtistTerms()
    {
        List<String> mostpopularArtistTerms = new ArrayList<>();
        Map<String, Float> genresFreq = getArtistTermsWithFreq();

        if (genresFreq != null)
        {
            Float popularArtistTermsFreq = -Float.MAX_VALUE;
            for (Map.Entry<String, Float> entry : genresFreq.entrySet()) {
                if (entry.getValue().compareTo(popularArtistTermsFreq) >= 0 && !mostpopularArtistTerms.contains(entry.getKey())) {
                    if (entry.getValue().compareTo(popularArtistTermsFreq) > 0) {
                        mostpopularArtistTerms.clear();
                        popularArtistTermsFreq = entry.getValue();
                    }
                    mostpopularArtistTerms.add(entry.getKey());
                }
            }
        }
        return mostpopularArtistTerms.size() > 0 ? mostpopularArtistTerms : null;
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

    public String getTitle()
    {
        return title;
    }

    public Integer getYear()
    {
        return year;
    }
}
