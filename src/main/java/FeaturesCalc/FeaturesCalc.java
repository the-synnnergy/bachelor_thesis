package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

    class InstanceData{
        double[] sim_scores;
        PostQueryCalc.PostQueryFeatures[] postq_features;
        PreQueryCalc.PrequeryFeatures[] preq_features;

        public InstanceData(){};

        public ArrayList<Pair<String,Double>> get_iterableList(){
            ArrayList<Pair<String,Double>> features = new ArrayList<>();
            for(Similarites sim : Similarites.values()){
                String name = sim.name()+"_score";
                features.add(new ImmutablePair<>(name,sim_scores[sim.ordinal()]));
            }
            for(Similarites sim : Similarites.values()){
                for(int i = 0;i< preq_features.length;i++){
                    String name = sim.name()+"_preq_";
                    features.addAll(preq_features[i].to_ArrayList_named(name));
                }
            }
            for(Similarites sim : Similarites.values()){
                for(int i = 0;i< postq_features.length;i++){
                    String name = sim.name()+"_preq_";
                    features.addAll(postq_features[i].to_ArrayList_named(name));
                }
            }
            return features;
        }
    }

    public static List<String> get_features()
    {
        return null;
    }

    public static Instance get_inst_to_classify_from_base(IndexReader[] readers, String query, String query_identfier, Directory[] query_indices) throws IOException
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

    public static Instance create_instance(InstanceData data){
        return null;
    }

    public static void to_arff_file(String filename, Instances dataset) throws Exception
    {
        ConverterUtils.DataSink.write(filename,dataset);
    }
}