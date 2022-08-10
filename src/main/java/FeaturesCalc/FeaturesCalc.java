package FeaturesCalc;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.util.QueryBuilder;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.IOException;
import java.util.*;

class IndexFeatureData
{
    // Maps for saving features calculation, prefix query saves results for queries from "query index" and target for "target index"
    Map<Integer, PostRetrievalCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
    Map<Integer, PostRetrievalCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
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

    /**
     * Calculates the full dataset for two sets of artifacts given as Apache Lucene indices
     * @param query_reader Source artifact indices
     * @param target_reader target artifact indices
     * @param stopwords stopwords
     * @return Instance Data dataset.
     * @throws IOException
     */
    public static List<InstanceData> get_full_dataset(IndexReader[] query_reader, IndexReader[] target_reader, String stopwords) throws IOException
    {
        assert Similarities.values().length == query_reader.length;
        assert Similarities.values().length == target_reader.length;
        IndexFeatureData[] data = new IndexFeatureData[Similarities.values().length];
        for (Similarities sim : Similarities.values())
        {
            int i = sim.ordinal();
            // precalculate the needed data to speed up the process
            data[i] = get_IndexInstanceData(query_reader[i], target_reader[i], null, sim);
        }
        List<InstanceData> instance_data = new ArrayList<>();
        PreRetrievalCalc preQueryCalcRetrieval = new PreRetrievalCalc(query_reader[0], new EnglishAnalyzer());
        PreRetrievalCalc preRetrievalCalcTarget = new PreRetrievalCalc(target_reader[0],new EnglishAnalyzer());
        DocumentStatisticsFeatures documentStatisticsFeatures = new DocumentStatisticsFeatures(query_reader[0],target_reader[0],new EnglishAnalyzer());
        Map<String, PreRetrievalCalc.PreretrievalFeatures> target_features = new HashMap<>();
        // calculate the pretrieval features
        for(int i = 0 ; i < target_reader[0].numDocs();i++)
        {
            target_features.put(target_reader[0].document(i).getField("title").stringValue(),get_preq_features(target_reader[0].document(i).getField("body").stringValue(), preQueryCalcRetrieval));
        }
        for (int i = 0; i < query_reader[0].numDocs(); i++)
        {
            System.out.println(query_reader);
            PreRetrievalCalc.PreretrievalFeatures preqFeaturesQuery =  get_preq_features(query_reader[0].document(i).getField("body").stringValue(), preRetrievalCalcTarget);
            for (int j = 0; j < target_reader[0].numDocs(); j++)
            {
                // create instance with the cached precalculated data
                InstanceData instance = new InstanceData();
                for (Similarities sim : Similarities.values())
                {
                    instance.sim_scores_query[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,true);
                    instance.sim_scores_target[sim.ordinal()] = data[sim.ordinal()].get_score_for_doc(i,j,false);
                    instance.postq_features_target[sim.ordinal()] = data[sim.ordinal()].target_postq_map.get(j);
                    instance.postq_features_query[sim.ordinal()] = data[sim.ordinal()].query_postq_map.get(i);

                }

                instance.preq_features_query = preqFeaturesQuery;
                instance.preq_features_target = target_features.getOrDefault(target_reader[0].document(j).getField("title").stringValue(),get_preq_features(target_reader[0].document(j).getField("body").stringValue(), preQueryCalcRetrieval));
                instance.identifier_query = query_reader[0].document(i).getField("title").stringValue();
                instance.identifier_target = target_reader[0].document(j).getField("title").stringValue();
                instance.documentStatistics = documentStatisticsFeatures.getDocumentStatisticsFeatures(instance.identifier_query, instance.identifier_target);
                instance_data.add(instance);
            }
        }
        return instance_data;
    }

    public static PreRetrievalCalc.PreretrievalFeatures get_preq_features(String query, PreRetrievalCalc preRetrievalCalc) throws IOException
    {
        return preRetrievalCalc.getPretrievalFeatures(query);
    }

    /**
     * Precalculates and caches data need for many calculations
     * @param query_reader Source artifact indices
     *  @param target_reader target artifact indices
     * @param stopwords stopwords
     * @param sim Similarity for the reader
     * @return Cached data as IndexFeatureData
     * @throws IOException
     */
    public static IndexFeatureData get_IndexInstanceData(IndexReader query_reader, IndexReader target_reader, String stopwords, Similarities sim) throws IOException
    {
        Map<Integer, TopDocs> query_top_docs = new HashMap<>();
        Map<Integer, TopDocs> target_top_docs = new HashMap<>();
        Map<Integer, PostRetrievalCalc.PostQueryFeatures> query_postq_map = new HashMap<>();
        Map<Integer, PreRetrievalCalc.PreretrievalFeatures> query_preq_map = new HashMap<>();
        Map<Integer, PostRetrievalCalc.PostQueryFeatures> target_postq_map = new HashMap<>();
        Map<Integer, PreRetrievalCalc.PreretrievalFeatures> target_preq_map = new HashMap<>();
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
        PostRetrievalCalc post_calc_query = new PostRetrievalCalc(target_reader, new EnglishAnalyzer(),luceneSim);
        PostRetrievalCalc post_calc_target = new PostRetrievalCalc(query_reader, new EnglishAnalyzer(),luceneSim);
        // precalculate the postretrieval features for every artifact and cache it in a map
        // do the same for retrieval score
        for (int i = 0; i < query_reader.numDocs(); i++)
        {
            String query = query_reader.document(i).getField("body").stringValue();
            query_postq_map.put(i, post_calc_query.getPostQueryFeatures(query));
        }
        for (int i = 0; i < target_reader.numDocs(); i++)
        {
            String query = target_reader.document(i).getField("body").stringValue();
            target_postq_map.put(i, post_calc_target.getPostQueryFeatures(query));
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