package a3.jobs;

import a3.data.Data;
import a3.io.StringTuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.*;

public class ArtistGenreAndSongCount
{
    public static class ArtistGenreAndSongCountMapper extends Mapper<Object, Text, Text, StringTuple>
    {
        private static final Text artistName = new Text();
        private static final StringTuple genresAndCount = new StringTuple();
        private static final String one = Integer.toString(1);

        @Override
        protected void map(Object o, Text contents, Context context) throws IOException, InterruptedException
        {
            Data d = new Data(contents.toString());
            if (d.isValid()) {
                String name = d.getArtistName();
                List<String> genres = d.getArtistTerms();
                if (genres == null) {
                    genres = new ArrayList<>();
                }
                else {
                    genresAndCount.set(genres);
                }

                //song count
                genres.add(one);

                if(name != null)
                {
                    artistName.set(name);
                    genresAndCount.set(genres);
                    context.write(artistName, genresAndCount);
                }
            }
        }
    }

    public static class ArtistGenreAndSongCountReducer extends Reducer<Text, StringTuple, Text, Text>
    {
        private static final Map<String, Integer> counts = new HashMap<>();
        private static final Set<String> mostCommonGenres = new HashSet<>();
        private static final Text results = new Text();

        @Override
        protected void reduce(Text artist, Iterable<StringTuple> genres, Context context) throws IOException, InterruptedException
        {
            counts.clear();
            mostCommonGenres.clear();

            int songCount = 0;
            for(StringTuple genreTuple: genres)
            {
                for(int i=0; i<genreTuple.size()-1 ; i++) {
                    String genre = genreTuple.get(i);
                    counts.putIfAbsent(genre, 0);
                    counts.replace(genre, counts.get(genre) + 1);
                }
                songCount += Integer.parseInt(genreTuple.get(genreTuple.size()-1));
            }

            Integer mostCommonGenreCount = Integer.MIN_VALUE;
            for(Map.Entry<String,Integer> entry : counts.entrySet()) {
                if(entry.getValue().compareTo(mostCommonGenreCount) > 0) {
                    mostCommonGenres.clear();
                    mostCommonGenres.add(entry.getKey());
                    mostCommonGenreCount = entry.getValue();
                }
                else if(entry.getValue().equals(mostCommonGenreCount))
                {
                    mostCommonGenres.add(entry.getKey());
                }
            }

            String mostCommonGenre = null;
            if (mostCommonGenres.size() > 0) {
                mostCommonGenre = String.join(", ", mostCommonGenres);
                mostCommonGenre += " (" + mostCommonGenreCount + ")";
            }
            results.set(String.format("%d\t%s", songCount, mostCommonGenre));
            if (mostCommonGenre != null)
                context.write(artist, results);
        }
    }


    /*
    public static class ArtistGenreAndSongCountCombiner extends Reducer<Text, StringTuple, Text, StringTuple>
    {
        private static final Map<String, Integer> counts = new HashMap<>();
        private static final Set<String> mostCommonGenres = new HashSet<>();
        private static final StringTuple results = new StringTuple();

        @Override
        protected void reduce(Text artist, Iterable<StringTuple> genres, Context context) throws IOException, InterruptedException
        {
            counts.clear();
            mostCommonGenres.clear();

            int songCount = 0;
            for(StringTuple genreTuple: genres)
            {
                for(int i=0; i<genreTuple.size()-1 ; i++) {
                    String genre = genreTuple.get(i);
                    counts.putIfAbsent(genre, 0);
                    counts.replace(genre, counts.get(genre) + 1);
                }
                songCount += Integer.parseInt(genreTuple.get(genreTuple.size()-1));
            }

            Integer mostCommonGenreCount = Integer.MIN_VALUE;
            for(Map.Entry<String,Integer> entry : counts.entrySet()) {
                if(entry.getValue().compareTo(mostCommonGenreCount)>0) {
                    mostCommonGenres.clear();
                    mostCommonGenres.add(entry.getKey());
                    mostCommonGenreCount = entry.getValue();
                }
                else if(entry.getValue().equals(mostCommonGenreCount))
                {
                    mostCommonGenres.add(entry.getKey());
                }
            }


            StringTuple s = new StringTuple(mostCommonGenres.toArray(new String[mostCommonGenres.size()]));
            for(int i=0; i<mostCommonGenreCount; i++)
                context.write(artist, s);
        }
    }
    */
    public static void main(String[] args)
    {
        try {
            Configuration conf = new Configuration();
            // Give the MapRed job a name. You'll see this name in the Yarn webapp.
            Job job = Job.getInstance(conf, "q1_7");
            // Current class.
            job.setJarByClass(ArtistGenreAndSongCount.class);
            // Mapper
            job.setMapperClass(ArtistGenreAndSongCountMapper.class);
            // Combiner.
            //job.setCombinerClass(ArtistGenreAndSongCountCombiner.class);
            // Reducer
            job.setReducerClass(ArtistGenreAndSongCountReducer.class);
            // Outputs from the Mapper.
            job.setMapOutputKeyClass(Text.class);
            job.setMapOutputValueClass(StringTuple.class);


            // Outputs from Reducer. It is sufficient to set only the following two properties
            // if the Mapper and Reducer has same key and value io. It is set separately for
            // elaboration.
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            // path to input in HDFS
            FileInputFormat.addInputPath(job, new Path(args[0]));
            // path to output in HDFS
            FileOutputFormat.setOutputPath(job, new Path(args[1]));
            // Block until the job is completed.
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

}
