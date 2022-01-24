package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.QueryBuilder;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class IndexFeatureData
{
    Map<Integer, PostQueryCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
    Map<Integer, PreQueryCalc.PrequeryFeatures> query_preq_map = new HashMap<>();
    Map<Integer, PostQueryCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
    Map<Integer, PreQueryCalc.PrequeryFeatures> target_preq_map = new HashMap<>();
    Map<Integer, TopDocs> query_top_docs = new HashMap<>();
    Map<Integer, TopDocs> target_top_docs = new HashMap<>();

    public IndexFeatureData()
    {
    }

    public float get_score_for_doc(int i, int j, boolean query_is_searched)
    {
        TopDocs top = null;
        if (query_is_searched)
        {
            top = query_top_docs.get(i);
        } else
        {
            top = target_top_docs.get(i);
        }
        for(ScoreDoc doc: top.scoreDocs){
            if(doc.doc == j) return doc.score;
        }
        return 0f;
    }

}

class InstanceData
{
    double[] sim_scores_query;
    double[] sim_scores_target;
    PostQueryCalc.PostQueryFeatures[] postq_features_query;
    PreQueryCalc.PrequeryFeatures[] preq_features_query;
    PostQueryCalc.PostQueryFeatures[] postq_features_target;
    PreQueryCalc.PrequeryFeatures[] preq_features_target;
    String identifier_query;
    String identifier_target;

    public InstanceData()
    {
    }

    public ArrayList<Pair<String, Double>> get_iterableList()
    {
        ArrayList<Pair<String, Double>> features = new ArrayList<>();
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            String name = sim.name() + "_query_score";
            features.add(new ImmutablePair<>(name, sim_scores_query[sim.ordinal()]));
        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            String name = sim.name() + "_target_score";
            features.add(new ImmutablePair<>(name, sim_scores_target[sim.ordinal()]));
        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PreQueryCalc.PrequeryFeatures preq_feature : preq_features_query)
            {
                String name = sim.name() + "_query_preq_";
                features.addAll(preq_feature.to_ArrayList_named(name));
            }
        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PostQueryCalc.PostQueryFeatures postq_feature : postq_features_query)
            {
                String name = sim.name() + "_query_preq_";
                features.addAll(postq_feature.to_ArrayList_named(name));
            }
        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PreQueryCalc.PrequeryFeatures preq_feature : preq_features_target)
            {
                String name = sim.name() + "_target_preq_";
                features.addAll(preq_feature.to_ArrayList_named(name));
            }
        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            for (PostQueryCalc.PostQueryFeatures postq_feature : postq_features_target)
            {
                String name = sim.name() + "_target_preq_";
                features.addAll(postq_feature.to_ArrayList_named(name));
            }
        }
        return features;
    }

    @Override
    public String toString(){
        List<Pair<String, Double>> list = get_iterableList();
        return list.stream().map(a -> String.valueOf(a.getRight())).collect(Collectors.joining(","));
    }
}

public class FeaturesCalc
{

    enum Similarities
    {
        CLASSIC_SIM,
        BM_25,
        DIRIRCHLET,
        JELINEK_MERCER
    }


    public static List<String> get_features()
    {
        return null;
    }

    /***
     *
     * @param non_query_readers
     * @param query
     * @param query_identfier
     * @param query_readers
     * @return
     * @throws IOException
     */
    public static Instance get_inst_to_classify_from_base(IndexReader[] non_query_readers, String query, String query_identfier, IndexReader[] query_readers) throws IOException
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
        for (FeaturesCalc.FeaturesCalc.Similarities sim : Similarities.values())
        {
            postquery[sim.ordinal()] = new PostQueryCalc(readers[sim.ordinal()], new EnglishAnalyzer());
            prequery[sim.ordinal()] = new PreQueryCalc(readers[sim.ordinal()], new EnglishAnalyzer());

        }
        for (FeaturesCalc.FeaturesCalc.Similarities sim : Similarities.values())
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

    public static Instance create_instance(InstanceData data)
    {
        return null;
    }

    public static void to_arff_file(String filename, Instances dataset) throws Exception
    {
        ConverterUtils.DataSink.write(filename, dataset);
    }


    public static Instances get_full_dataset(IndexReader[] query_reader, IndexReader[] target_reader, Analyzer anal) throws IOException
    {
        assert Similarities.values().length == query_reader.length;
        assert Similarities.values().length == target_reader.length;
        IndexFeatureData[] data = new IndexFeatureData[Similarities.values().length];
        for (Similarities sim : Similarities.values())
        {
            int i = sim.ordinal();
            data[i] = get_IndexInstanceData(query_reader[i], target_reader[i], anal);
        }
        // Assert all indices have the same length
        List<InstanceData> instance_data = new ArrayList<>();
        // assert all readers have same length, better check here, if not throw exception!
        for (int i = 0; i < query_reader[0].numDocs(); i++)
        {
            for (int j = 0; j < target_reader[0].numDocs(); j++)
            {
                InstanceData instance = new InstanceData();
                for (Similarities sim : Similarities.values())
                {
                    instance.sim_scores_query[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,true);
                    instance.sim_scores_target[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,false);
                    instance.postq_features_target[sim.ordinal()] = data[sim.ordinal()].target_postq_map.get(j);
                    instance.preq_features_target[sim.ordinal()] = data[sim.ordinal()].target_preq_map.get(j);
                    instance.postq_features_query[sim.ordinal()] = data[sim.ordinal()].query_postq_map.get(i);
                    instance.preq_features_query[sim.ordinal()] = data[sim.ordinal()].query_preq_map.get(j);
                }
                instance.identifier_query = query_reader[0].document(i).getField("name").stringValue();
                instance.identifier_target = target_reader[0].document(j).getField("name").stringValue();
                instance_data.add(instance);
            }
        }
    }

    public static IndexFeatureData get_IndexInstanceData(IndexReader query_reader, IndexReader target_reader, Analyzer anal) throws IOException
    {
        Map<Integer, PostQueryCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
        Map<Integer, PreQueryCalc.PrequeryFeatures> query_preq_map = new HashMap<>();
        Map<Integer, PostQueryCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
        Map<Integer, PreQueryCalc.PrequeryFeatures> target_preq_map = new HashMap<>();
        Map<Integer, TopDocs> query_top_docs = new HashMap<>();
        Map<Integer, TopDocs> target_top_docs = new HashMap<>();
        PreQueryCalc pre_calc_query = new PreQueryCalc(target_reader, anal);
        PreQueryCalc pre_calc_target = new PreQueryCalc(target_reader, anal);
        PostQueryCalc post_calc_query = new PostQueryCalc(target_reader, anal);
        PostQueryCalc post_calc_target = new PostQueryCalc(query_reader, anal);
        for (int i = 0; i < query_reader.numDocs(); i++)
        {
            String query = query_reader.document(i).getField("body").stringValue();
            query_postq_map.put(i, post_calc_query.get_PostQueryFeatures(query));
            query_preq_map.put(i, pre_calc_query.get_prequery_features(query));
        }
        for (int i = 0; i < target_reader.numDocs(); i++)
        {
            String query = target_reader.document(i).getField("body").stringValue();
            query_postq_map.put(i, post_calc_target.get_PostQueryFeatures(query));
            query_preq_map.put(i, pre_calc_target.get_prequery_features(query));
        }
        get_top_docs_to_map(query_reader, target_reader, anal, query_top_docs);
        get_top_docs_to_map(target_reader, query_reader, anal, target_top_docs);
        IndexFeatureData data = new IndexFeatureData();
        data.query_postq_map = query_postq_map;
        data.query_preq_map = query_preq_map;
        data.query_top_docs = query_top_docs;
        data.target_top_docs = target_top_docs;
        data.target_postq_map = target_postq_map;
        data.target_preq_map = target_preq_map;
        return data;
    }

    private static void get_top_docs_to_map(IndexReader query_reader, IndexReader target_reader, Analyzer anal, Map<Integer, TopDocs> query_top_docs) throws IOException
    {
        for (int i = 0; i < query_reader.numDocs(); i++)
        {
            String query_raw = query_reader.document(i).getField("body").stringValue();
            QueryBuilder qb = new QueryBuilder(anal);
            Query query = qb.createBooleanQuery("body", query_raw);
            TopDocs result = (new IndexSearcher(target_reader)).search(query, Integer.MAX_VALUE);
            query_top_docs.put(i, result);
        }
    }
}