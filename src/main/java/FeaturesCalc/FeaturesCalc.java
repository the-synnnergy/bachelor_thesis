package FeaturesCalc;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import weka.core.Instance;

import java.io.IOException;
import java.util.List;

public class FeaturesCalc
{
    public static List<String> get_features()
    {
        return null;
    }

    public static Instance get_inst_to_classify(IndexReader[] readers, String query, String query_identfier, Directory[] query_indices) throws IOException
    {
        IndexWriterConfig conf = new IndexWriterConfig(new EnglishAnalyzer());
        conf.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        IndexWriter[] query_indices_writers = new IndexWriter[query_indices.length];
        for(int i = 0; i< query_indices_writers.length;i++){
            query_indices_writers[i] = new IndexWriter(query_indices[i],conf);
        }

        return null;
    }


}
