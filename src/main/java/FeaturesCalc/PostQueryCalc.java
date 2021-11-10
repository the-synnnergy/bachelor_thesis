package FeaturesCalc;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static FeaturesCalc.Util.*;

public class PostQueryCalc
{
    /**
     * Class for storing the features, so they can be returned and then easily extracted.
     */

    public class PostQueryFeatures
    {
        double subquery_overlap;
        double robustness_score;
        double first_rank_change;
        double clustering_tendency;
        double spatial_autocorrelation;
        double weighted_information_gain;
        double normalized_query_commitment;

        public void print()
        {
            System.out.println("subquery_overlap:" + subquery_overlap);
            System.out.println("robustness_score:" + robustness_score);
            System.out.println("first_rank_change:" + first_rank_change);
            System.out.println("clustering_tendency:" + clustering_tendency);
            System.out.println("spatial_autocorreleation:" + spatial_autocorrelation);
            System.out.println("WIG:" + weighted_information_gain);
            System.out.println("Normalized query commitment:" + normalized_query_commitment);
        }
    }

    IndexReader reader;
    Map<Integer, double[]> document_to_termvectors;
    private static final int MAX_HITS_SUBQUERY = 10;
    private static final int MAX_HITS_ROBUSTNESS_SCORE = 50;
    private static final int MAX_HITS_WEIGHTED_INFORMATION_GAIN = 100;
    private static final int MAX_HITS_CLUSTERING = 100;
    private int collection_size;

    public PostQueryCalc(IndexReader reader) throws IOException
    {
        this.reader = reader;
        document_to_termvectors = Util.get_idf_document_vectors(reader);
    }

    public PostQueryFeatures get_PostQueryFeatures(String query) throws IOException
    {
        PostQueryFeatures features = new PostQueryFeatures();
        IndexSearcher searcher = new IndexSearcher(reader);
        long time = System.currentTimeMillis();
        features.clustering_tendency = get_clustering_tendency(query, searcher);
        System.out.println("Clustering Tendency:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.first_rank_change = get_first_rank_change(query, searcher);
        System.out.println("First Rank change:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.normalized_query_commitment = get_normalized_query_commitment(query, searcher);
        System.out.println("Normalized query commitment:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.robustness_score = get_robustness_score(query, searcher);
        System.out.println("Get robustness score:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.spatial_autocorrelation = get_spatial_autocorrelation(query, searcher);
        System.out.println("Spatial autocorrelation:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.subquery_overlap = get_subquery_overlap(query, searcher);
        System.out.println("subquery overlap:" + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        features.weighted_information_gain = get_weighted_information_gain(query, searcher);
        System.out.println("wig:" + (System.currentTimeMillis() - time));
        return features;
    }

    /**
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */

    private double get_subquery_overlap(String query, IndexSearcher searcher) throws IOException
    {
        // get first then hits from query.
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body", query);
        TopDocs initial_result = searcher.search(bool_query, MAX_HITS_SUBQUERY);
        List<Integer> initial_indizes = new ArrayList<>();
        for (ScoreDoc hit : initial_result.scoreDocs)
        {
            initial_indizes.add(hit.doc);
        }
        List<Double> overlapping_results = new ArrayList<>();
        for (BooleanClause clause : ((BooleanQuery) bool_query).clauses())
        {
            double overlapping_results_tmp = 0;
            TopDocs tmp_results = searcher.search(clause.getQuery(), MAX_HITS_SUBQUERY);
            for (ScoreDoc hit : tmp_results.scoreDocs)
            {
                if (initial_indizes.contains(hit.doc)) overlapping_results_tmp++;
            }
            overlapping_results.add(overlapping_results_tmp);
        }
        return new StandardDeviation().evaluate(overlapping_results.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private double get_robustness_score(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body", query);
        TopDocs initial_result = searcher.search(bool_query, MAX_HITS_ROBUSTNESS_SCORE);
        List<Integer> ranked_list = new ArrayList<>();
        for (ScoreDoc hit : initial_result.scoreDocs)
        {
            ranked_list.add(hit.doc);
        }
        List<Term> terms_list = extract_terms_boolean_query((BooleanQuery) bool_query);
        // construct pertubed query
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        query_builder.setMinimumNumberShouldMatch(1);
        for (Term term : terms_list)
        {
            query_builder.add(new PertubedQuery(term, ranked_list), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubed_query = query_builder.build();
        List<Double> spearman_rank_correlations = new ArrayList<>();
        for (int i = 0; i < 100; i++)
        {
            TopDocs result = searcher.search(pertubed_query, MAX_HITS_ROBUSTNESS_SCORE);
            List<Integer> pertubed_ranked_list = new ArrayList<>();
            for (ScoreDoc hit : result.scoreDocs)
            {
                pertubed_ranked_list.add(hit.doc);
            }
            double tmp_spearman = new SpearmansCorrelation().correlation(ranked_list.stream().mapToDouble(d -> d).toArray(), pertubed_ranked_list.stream().mapToDouble(d -> d).toArray());
            spearman_rank_correlations.add(tmp_spearman);
        }
        return spearman_rank_correlations.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }


    private double get_first_rank_change(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body", query);
        TopDocs initial_result = searcher.search(bool_query, MAX_HITS_ROBUSTNESS_SCORE);
        int top_hit = initial_result.scoreDocs[0].doc;
        List<Integer> ranked_list = new ArrayList<>();
        for (ScoreDoc hit : initial_result.scoreDocs)
        {
            ranked_list.add(hit.doc);
        }
        List<Term> terms_list = extract_terms_boolean_query((BooleanQuery) bool_query);
        // construct pertubed query
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        query_builder.setMinimumNumberShouldMatch(1);
        for (Term term : terms_list)
        {
            query_builder.add(new PertubedQuery(term, ranked_list), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubed_query = query_builder.build();
        int first_rank_change_sum = 0;
        for (int i = 0; i < 100; i++)
        {
            TopDocs result = searcher.search(pertubed_query, MAX_HITS_ROBUSTNESS_SCORE);
            if (result.scoreDocs[0].doc == top_hit) first_rank_change_sum++;
        }
        return first_rank_change_sum;
    }

    private double get_clustering_tendency(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body", query);
        TopDocs top100 = searcher.search(bool_query, MAX_HITS_CLUSTERING);
        Set<Integer> doc_ids = new HashSet<>();
        for (ScoreDoc top_doc : top100.scoreDocs)
        {
            doc_ids.add(top_doc.doc);
        }
        Set<Integer> sampleable_points = new HashSet<>();
        TopDocs all_docs = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
        for (ScoreDoc scoredoc : all_docs.scoreDocs)
        {
            sampleable_points.add(scoredoc.doc);
        }
        // Now its time to remove all Docs already in the top 100 from sampling!
        sampleable_points.removeAll(doc_ids);
        Map<Integer, double[]> term_vectors = get_idf_document_vectors(reader);
        double mean = 0;
        for (int i = 0; i < 100; i++)
        {
            mean += get_sim_query_for_mean(sampleable_points, searcher, doc_ids, top100, term_vectors, query);
        }
        mean = mean / 100;
        int termvector_length = term_vectors.get(0).length;
        double[] max = new double[termvector_length];
        double[] min = new double[termvector_length];
        Arrays.fill(min, Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : top100.scoreDocs)
        {
            double[] termvector = term_vectors.get(scoreDoc.doc);
            for (int i = 0; i < termvector_length; i++)
            {
                if (termvector[i] > max[i]) max[i] = termvector[i];
                if (termvector[i] < min[i]) min[i] = termvector[i];
            }
        }
        double sum = 0;
        for (int i = 0; i < termvector_length; i++)
        {
            sum += max[i] - min[i];
        }
        return mean * (1.0d / termvector_length) * sum;
    }

    private double get_sim_query_for_mean(Set<Integer> sampleable_points, IndexSearcher searcher, Set<Integer> doc_ids, TopDocs top100, Map<Integer, double[]> term_vectors, String query) throws IOException
    {
        int sampled_point = sampleable_points.stream().skip(ThreadLocalRandom.current().nextInt(sampleable_points.size())).findFirst().orElseThrow();
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query sampled_point_query = qb.createBooleanQuery("body", (reader.document(sampled_point).get("body")));
        TopDocs result_docs = searcher.search(sampled_point_query, Integer.MAX_VALUE);
        int marked_point = 0;
        for (ScoreDoc result : result_docs.scoreDocs)
        {
            if (doc_ids.contains(result.doc))
            {
                marked_point = result.doc;
                break;
            }
        }
        int marked_point_index = 0;
        for (int i = 0; i < top100.scoreDocs.length; i++)
        {
            if (top100.scoreDocs[i].doc == marked_point)
            {
                marked_point_index = i;
                //System.out.println("Found marked point");
                break;
            }
        }
        int nearest_neighbor;
        switch (marked_point_index)
        {
            case 0:
                nearest_neighbor = 1;
                break;
            case MAX_HITS_CLUSTERING:
                nearest_neighbor = MAX_HITS_CLUSTERING - 1;
                break;
            default:
                if (top100.scoreDocs[marked_point_index - 1].score > (top100.scoreDocs[marked_point_index + 1].score))
                {
                    nearest_neighbor = marked_point_index - 1;
                } else
                {
                    nearest_neighbor = marked_point_index + 1;
                }
        }
        int nearest_neighbor_doc_id = top100.scoreDocs[nearest_neighbor].doc;
        int marked_point_doc_id = top100.scoreDocs[marked_point_index].doc;
        //System.out.println(term_vectors.get(nearest_neighbor_doc_id)[265]);
        //System.out.println(marked_point_doc_id);
        double sim_query_dmp_dnn = calc_sim_query(term_vectors.get(marked_point_doc_id), term_vectors.get(nearest_neighbor_doc_id), get_query_idf_termvector(query, reader));
        double sim_query_psp_dmp = calc_sim_query(term_vectors.get(sampled_point), term_vectors.get(marked_point_doc_id), get_query_idf_termvector(query, reader));


        //System.out.println("sim query results:"+sim_query_dmp_dnn+","+sim_query_psp_dmp);
        return sim_query_dmp_dnn / sim_query_psp_dmp;
    }

    private double calc_sim_query(double[] first, double[] second, double[] query_termvector)
    {
        // maybe do some assertions here
        //System.out.println("lenght of vectors(calc_sim_query):" + first.length + "," + second.length + "," + query_termvector.length);
        assert query_termvector.length == first.length;
        assert first.length == second.length;
        double sim_query_upper_part1 = 0;
        double sim_query_lower_part1_sum1 = 0;
        double sim_query_lower_part1_sum2 = 0;
        double sim_query_upper_part2 = 0;
        double sim_query_lower_part2_sum1 = 0;
        double sim_query_lower_part2_sum2 = 0;
        for (int i = 0; i < first.length; i++)
        {
            double first_freq = first[i];
            double second_freq = second[i];
            sim_query_upper_part1 += first_freq * second_freq;
            sim_query_lower_part1_sum1 += Math.pow(first_freq, 2);
            sim_query_lower_part1_sum2 += Math.pow(second_freq, 2);
            //System.out.println("query-termvector:" + query_termvector[i]);
            sim_query_upper_part2 += ((first_freq + second_freq) / 2) * query_termvector[i];
            sim_query_lower_part2_sum1 += Math.pow(((first_freq + second_freq) / 2), 2);
            sim_query_lower_part2_sum2 += Math.pow(query_termvector[i], 2);
        }
        double sim_query_part1 = sim_query_upper_part1 / (sim_query_lower_part1_sum1 * sim_query_lower_part1_sum2);
        double sim_query_part2 = sim_query_upper_part2 / (sim_query_lower_part2_sum1 * sim_query_lower_part2_sum2);
        //System.out.println("calc_sim_query" + sim_query_part1 + "," + sim_query_part2);
        //System.out.println("result in sim_query_calc:" + sim_query_part1 * sim_query_part2);
        return sim_query_part1 * sim_query_part2;
    }

    private double get_spatial_autocorrelation(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body", query);
        TopDocs hits = searcher.search(bool_query, 50);
        double[] original_scores = new double[hits.scoreDocs.length];
        for (int i = 0; i < hits.scoreDocs.length; i++)
        {
            original_scores[i] = hits.scoreDocs[i].score;
        }
        double[] new_scores = new double[hits.scoreDocs.length];
        for (int i = 0; i < hits.scoreDocs.length; i++)
        {
            List<Double> cos_similarities = new ArrayList<>();
            // Calculate cosine sim for each
            double[] termvector_i = document_to_termvectors.get(hits.scoreDocs[i].doc);
            for (int j = 0; j < hits.scoreDocs.length; j++)
            {
                if (j == i) continue;

                double[] termvector_j = document_to_termvectors.get(hits.scoreDocs[j].doc);
                double cos_score = Util.cos_sim(termvector_i, termvector_j);
                cos_similarities.add(cos_score);
            }
            cos_similarities.sort(Comparator.naturalOrder());
            int size = Math.min(cos_similarities.size(), 5);
            for (int k = 0; k < size; k++)
            {
                new_scores[i] += cos_similarities.get(k);
            }
            new_scores[i] = new_scores[i] / size;
        }

        return new PearsonsCorrelation().correlation(original_scores, new_scores);
    }


    /**
     * @param query
     * @return
     */

    private double get_weighted_information_gain(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query q = qb.createBooleanQuery("body", query);
        TopDocs top_hits = searcher.search(q, MAX_HITS_WEIGHTED_INFORMATION_GAIN);
        //TopDocs all_hits = searcher.search(q, Integer.MAX_VALUE);
        List<String> query_terms = get_query_terms(query, reader, new EnglishAnalyzer());
        double lambda = 1 / Math.sqrt(query_terms.size());
        double weighted_information_gain = 0;
        Map<String, Double> corpus_probs = get_corpus_probs(reader);
        Map<Integer, Map<String, Double>> per_document_probs = Util.get_document_probs(reader);
        for (ScoreDoc scoreDoc : top_hits.scoreDocs)
        {
            Map<String, Double> term_probabilities_document = per_document_probs.get(scoreDoc.doc);
            System.out.println();
            for (String term : query_terms)
            {
                weighted_information_gain += lambda * Math.log(term_probabilities_document.getOrDefault(term, 0.0d) / corpus_probs.get(term));
            }
        }
        return weighted_information_gain / top_hits.scoreDocs.length;
    }

    /**
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */

    private double get_normalized_query_commitment(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query q = qb.createBooleanQuery("body", query);
        TopDocs top_hits = searcher.search(q, 100);
        TopDocs all_hits = searcher.search(q, Integer.MAX_VALUE);
        double mu = 0;
        for (ScoreDoc score_doc : top_hits.scoreDocs)
        {
            mu += score_doc.score;
        }
        mu = mu / top_hits.scoreDocs.length;
        double total_score_sum = 0;
        for (ScoreDoc scoreDoc : all_hits.scoreDocs)
        {
            total_score_sum += scoreDoc.score;
        }
        double tmp_upper_deviation = 0;
        for (ScoreDoc score_doc : top_hits.scoreDocs)
        {
            tmp_upper_deviation += Math.pow((score_doc.score - mu), 2);
        }
        tmp_upper_deviation = tmp_upper_deviation / 100;
        tmp_upper_deviation = Math.sqrt(tmp_upper_deviation);
        return tmp_upper_deviation / total_score_sum;
    }

    private List<Term> extract_terms_boolean_query(BooleanQuery query)
    {
        List<Term> terms = new ArrayList<>();
        for (BooleanClause clause : query.clauses())
        {
            terms.add(((TermQuery) clause.getQuery()).getTerm());
        }
        return terms;
    }


}

