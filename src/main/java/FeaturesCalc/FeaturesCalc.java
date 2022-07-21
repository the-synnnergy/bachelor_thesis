package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.util.QueryBuilder;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class IndexFeatureData
{
    // Maps for saving features calculation, prefix query saves results for queries from "query index" and target for "target index"
    Map<Integer, PostQueryCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
    Map<Integer, PostQueryCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
    Map<Integer, TopDocs> query_top_docs = new HashMap<>();
    Map<Integer, TopDocs> target_top_docs = new HashMap<>();

    public IndexFeatureData()
    {
    }

    public float get_score_for_doc(int query_doc_id, int target_doc_id, boolean query_is_searched)
    {
        TopDocs top = null;
        if (query_is_searched)
        {
            top = query_top_docs.get(query_doc_id);
            for(ScoreDoc doc: top.scoreDocs){
                if(doc.doc == target_doc_id) return doc.score;
            }
        }else
        {
            top = target_top_docs.get(target_doc_id);
            for(ScoreDoc doc: top.scoreDocs){
                if(doc.doc == query_doc_id) return doc.score;
            }
        }

        return 0f;
    }

}

public class FeaturesCalc
{

    public enum Similarities
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
    public static Instances get_insts_to_classify_from_lucene(IndexReader[] non_query_readers, String query, String query_identfier, IndexReader[] query_readers) throws IOException
    {
        // Arrays for Postquery and Prequery Features

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


    public static List<InstanceData> get_full_dataset(IndexReader[] query_reader, IndexReader[] target_reader, String stopwords) throws IOException
    {
        assert Similarities.values().length == query_reader.length;
        assert Similarities.values().length == target_reader.length;
        IndexFeatureData[] data = new IndexFeatureData[Similarities.values().length];
        for (Similarities sim : Similarities.values())
        {
            int i = sim.ordinal();
            // # TODO prequery only once and not for every
            data[i] = get_IndexInstanceData(query_reader[i], target_reader[i], null, sim);
        }
        // Assert all indices have the same length
        List<InstanceData> instance_data = new ArrayList<>();
        // assert all readers have same length, better check here, if not throw exception!
        PreQueryCalc preQueryCalcQuery = new PreQueryCalc(query_reader[0], new EnglishAnalyzer());
        PreQueryCalc preQueryCalcTarget = new PreQueryCalc(target_reader[0],new EnglishAnalyzer());
        DocumentStatisticsFeatures documentStatisticsFeatures = new DocumentStatisticsFeatures(query_reader[0],target_reader[0],new EnglishAnalyzer());
        Map<String,PreQueryCalc.PrequeryFeatures> target_features = new HashMap<>();
        for(int i = 0 ; i < target_reader[0].numDocs();i++)
        {
            target_features.put(target_reader[0].document(i).getField("title").stringValue(),get_preq_features(target_reader[0].document(i).getField("body").stringValue(),preQueryCalcQuery));
        }
        for (int i = 0; i < query_reader[0].numDocs(); i++)
        {
            System.out.println(query_reader);
            PreQueryCalc.PrequeryFeatures  preqFeaturesQuery =  get_preq_features(query_reader[0].document(i).getField("body").stringValue(),preQueryCalcTarget);
            for (int j = 0; j < target_reader[0].numDocs(); j++)
            {
                InstanceData instance = new InstanceData();
                for (Similarities sim : Similarities.values())
                {
                    instance.sim_scores_query[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,true);
                    instance.sim_scores_target[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,false);
                    instance.postq_features_target[sim.ordinal()] = data[sim.ordinal()].target_postq_map.get(j);
                    instance.postq_features_query[sim.ordinal()] = data[sim.ordinal()].query_postq_map.get(i);

                }

                instance.preq_features_query = preqFeaturesQuery;
                instance.preq_features_target = target_features.getOrDefault(target_reader[0].document(j).getField("title").stringValue(),get_preq_features(target_reader[0].document(j).getField("body").stringValue(),preQueryCalcQuery));
                instance.identifier_query = query_reader[0].document(i).getField("title").stringValue();
                instance.identifier_target = target_reader[0].document(j).getField("title").stringValue();
                instance.documentStatistics = documentStatisticsFeatures.getDocumentStatisticsFeatures(instance.identifier_query, instance.identifier_target);
                instance_data.add(instance);
            }
        }
        return instance_data;
    }

    public static PreQueryCalc.PrequeryFeatures get_preq_features(String query, PreQueryCalc preQueryCalc) throws IOException
    {
        return preQueryCalc.get_prequery_features(query);
    }


    public static IndexFeatureData get_IndexInstanceData(IndexReader query_reader, IndexReader target_reader, String stopwords, Similarities sim) throws IOException
    {
        Map<Integer, TopDocs> query_top_docs = new HashMap<>();
        Map<Integer, TopDocs> target_top_docs = new HashMap<>();
        Map<Integer, PostQueryCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
        Map<Integer, PreQueryCalc.PrequeryFeatures> query_preq_map = new HashMap<>();
        Map<Integer, PostQueryCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
        Map<Integer, PreQueryCalc.PrequeryFeatures> target_preq_map = new HashMap<>();
        Similarity luceneSim = null;
        switch (sim)
        {
            case DIRIRCHLET:
                luceneSim = new LMDirichletSimilarity();
                break;
            case CLASSIC_SIM:
                luceneSim = new ClassicSimilarity();
                break;
            case BM_25:
                luceneSim = new BM25Similarity();
                break;
            case JELINEK_MERCER:
                luceneSim = new LMJelinekMercerSimilarity(0.7f);
                break;
        }
        PostQueryCalc post_calc_query = new PostQueryCalc(target_reader, new EnglishAnalyzer(),luceneSim);
        PostQueryCalc post_calc_target = new PostQueryCalc(query_reader, new EnglishAnalyzer(),luceneSim);
        for (int i = 0; i < query_reader.numDocs(); i++)
        {
            String query = query_reader.document(i).getField("body").stringValue();
            query_postq_map.put(i, post_calc_query.get_PostQueryFeatures(query));
        }
        for (int i = 0; i < target_reader.numDocs(); i++)
        {
            String query = target_reader.document(i).getField("body").stringValue();
            target_postq_map.put(i, post_calc_target.get_PostQueryFeatures(query));
        }
        get_top_docs_to_map(query_reader, target_reader, new EnglishAnalyzer(), query_top_docs, luceneSim);
        get_top_docs_to_map(target_reader, query_reader, new EnglishAnalyzer(), target_top_docs,luceneSim);
        IndexFeatureData data = new IndexFeatureData();
        data.query_postq_map = query_postq_map;
        data.query_top_docs = query_top_docs;
        data.target_top_docs = target_top_docs;
        data.target_postq_map = target_postq_map;
        return data;
    }

    private static void get_top_docs_to_map(IndexReader query_reader, IndexReader target_reader, Analyzer anal, Map<Integer, TopDocs> query_top_docs, Similarity sim) throws IOException
    {

        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        IndexSearcher searcher = new IndexSearcher(query_reader);
        searcher.setSimilarity(sim);
        for (int i = 0; i < query_reader.numDocs(); i++)
        {
            String query_raw = query_reader.document(i).getField("body").stringValue();
            QueryBuilder qb = new QueryBuilder(anal);
            Query query = qb.createBooleanQuery("body", query_raw);
            TopDocs result = searcher.search(query, Integer.MAX_VALUE);
            query_top_docs.put(i, result);
        }
    }
}