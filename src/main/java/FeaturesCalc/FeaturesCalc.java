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

    enum Similarites
    {
        CLASSIC_SIM,
        BM_25,
        DIRIRCHLET,
        JELINEK_MERCER;
    }

    public static List<String> get_features()
    {
        return null;
    }

    public static Instance get_inst_to_classify(IndexReader[] readers, String query, String query_identfier, Directory[] query_indices) throws IOException
    {
        PostQueryCalc.PostQueryFeatures[] postq_features = new PostQueryCalc.PostQueryFeatures[readers.length];
        PreQueryCalc.PrequeryFeatures[] preq_features = new PreQueryCalc.PrequeryFeatures[readers.length];
        PostQueryCalc[] postquery = new PostQueryCalc[readers.length];
        PreQueryCalc[] prequery = new PreQueryCalc[readers.length];
        double[] sim_scores = new double[readers.length];
        IndexWriterConfig conf = new IndexWriterConfig(new EnglishAnalyzer());
        conf.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        IndexWriter[] query_indices_writers = new IndexWriter[query_indices.length];
        for (int i = 0; i < query_indices_writers.length; i++)
        {
            query_indices_writers[i] = new IndexWriter(query_indices[i], conf);

        }
        for (Similarites sim : Similarites.values())
        {
            postquery[sim.ordinal()] = new PostQueryCalc(readers[sim.ordinal()], new EnglishAnalyzer());
            prequery[sim.ordinal()] = new PreQueryCalc(readers[sim.ordinal()], new EnglishAnalyzer());
        }
        for (Similarites sim : Similarites.values())
        {
            postq_features[sim.ordinal()] = postquery[sim.ordinal()].get_PostQueryFeatures(query);
            preq_features[sim.ordinal()] = prequery[sim.ordinal()].get_prequery_features(query);
        }
        /**
         * #TODO
         * 1. Add query to the writers!
         * 2. Prune List of results! by eliminating
         * 3. Get both scores from index
         * 4. Get Features from Writer side!
         * 5. Constructs instances! ( Method in util maybe? or here)
         */
        return null;
    }


}
