import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class PostQueryCalc {
    /**
     * Class for storing the features, so they can be returned and then easily extracted.
     */
    public class PostQueryFeatures{
        double subquery_overlap;
        double robustness_score;
        double first_rank_change;
        double clustering_tendency;
        double spatial_autocorrelation;
        double weighted_information_gain;
        double normalized_query_commitment;
    }

    IndexReader reader;
    HashMap<Integer,Double[]> document_to_termvectors;
    private static final int MAX_HITS_SUBQUERY = 10;
    private static final int MAX_HITS_ROBUSTNESS_SCORE = 50;
    public PostQueryCalc(IndexReader reader) {
        this.reader = reader;
        // #TODO fill termvectors....
    }

    public PostQueryFeatures get_PostQueryFeatures(String query) throws IOException {
        PostQueryFeatures features = new PostQueryFeatures();
        IndexSearcher searcher = new IndexSearcher(reader);
        features.clustering_tendency = get_clustering_tendency(query);
        features.first_rank_change = get_first_rank_change(query, searcher);
        features.normalized_query_commitment = get_normalized_query_commitment(query);
        features.robustness_score = get_robustness_score(query, searcher);
        features.spatial_autocorrelation = get_spatial_autocorrelation(query, searcher);
        features.subquery_overlap = get_subquery_overlap(query, searcher);
        features.weighted_information_gain = get_weighted_information_gain(query);
        return features;
    }

    /**
     *
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */
    private double get_subquery_overlap(String query, IndexSearcher searcher) throws IOException {
        // get first then hits from query.
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body",query);
        TopDocs initial_result =searcher.search(bool_query, MAX_HITS_SUBQUERY);
        List<Integer> initial_indizes= new ArrayList<>();
        for(ScoreDoc hit: initial_result.scoreDocs){
            initial_indizes.add(hit.doc);
        }
        List<Double> overlapping_results = new ArrayList<>();
        for(BooleanClause clause: ((BooleanQuery)bool_query).clauses()){
            double overlapping_results_tmp = 0;
            TopDocs tmp_results = searcher.search(clause.getQuery(),MAX_HITS_SUBQUERY);
            for(ScoreDoc hit : tmp_results.scoreDocs){
                if(initial_indizes.contains(hit.doc)) overlapping_results_tmp++;
            }
            overlapping_results.add(overlapping_results_tmp);
        }
        return new StandardDeviation().evaluate(overlapping_results.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private double get_robustness_score(String query, IndexSearcher searcher) throws IOException {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body",query);
        TopDocs initial_result =searcher.search(bool_query, MAX_HITS_ROBUSTNESS_SCORE);
        List<Integer> ranked_list= new ArrayList<>();
        for(ScoreDoc hit: initial_result.scoreDocs){
            ranked_list.add(hit.doc);
        }
        List<Term> terms_list = extract_terms((BooleanQuery) bool_query);
        // construct pertubed query
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        query_builder.setMinimumNumberShouldMatch(1);
        for(Term term:terms_list){
            query_builder.add(new PertubedQuery(term,ranked_list),BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubed_query = query_builder.build();
        List<Double> spearman_rank_correlations = new ArrayList<>();
        for(int i = 0; i< 100;i++){
            TopDocs result = searcher.search(pertubed_query,MAX_HITS_ROBUSTNESS_SCORE);
            List<Integer> pertubed_ranked_list = new ArrayList<>();
            for(ScoreDoc hit: result.scoreDocs){
                pertubed_ranked_list.add(hit.doc);
            }
            double tmp_spearman =new SpearmansCorrelation().correlation(ranked_list.stream().mapToDouble(d->d).toArray(),pertubed_ranked_list.stream().mapToDouble(d->d).toArray());
            spearman_rank_correlations.add(tmp_spearman);
        }
        return spearman_rank_correlations.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private double get_first_rank_change(String query, IndexSearcher searcher) throws IOException {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body",query);
        TopDocs initial_result =searcher.search(bool_query, MAX_HITS_ROBUSTNESS_SCORE);
        int top_hit = initial_result.scoreDocs[0].doc;
        List<Integer> ranked_list= new ArrayList<>();
        for(ScoreDoc hit: initial_result.scoreDocs){
            ranked_list.add(hit.doc);
        }
        List<Term> terms_list = extract_terms((BooleanQuery) bool_query);
        // construct pertubed query
        BooleanQuery.Builder query_builder = new BooleanQuery.Builder();
        query_builder.setMinimumNumberShouldMatch(1);
        for(Term term:terms_list){
            query_builder.add(new PertubedQuery(term,ranked_list),BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubed_query = query_builder.build();
        int first_rank_change_sum = 0;
        for(int i = 0; i<100;i++){
            TopDocs result = searcher.search(pertubed_query,MAX_HITS_ROBUSTNESS_SCORE);
            if(result.scoreDocs[0].doc == top_hit) first_rank_change_sum++;
        }
        return first_rank_change_sum;
    }

    private double get_clustering_tendency(String query){

        return 0d;
    }

    private double get_spatial_autocorrelation(String query, IndexSearcher searcher) throws IOException {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query bool_query = qb.createBooleanQuery("body",query);
        TopDocs hits = searcher.search(bool_query,50);
        double[] original_scores = new double[hits.scoreDocs.length];
        for(int i = 0; i<hits.scoreDocs.length;i++){
            original_scores[i] = hits.scoreDocs[i].score;
        }
        double[] new_scores = new double[hits.scoreDocs.length];
        for(int i = 0; i<hits.scoreDocs.length; i++){
            List<Double> cos_similarities = new ArrayList<>();
            // Calculate cosine sim for each
            for(int j = 0; j<hits.scoreDocs.length;j++){
                if(j == i) continue;
                double cos_score = cos_sim(document_to_termvectors.get(i),document_to_termvectors.get(j));
                cos_similarities.add(cos_score);
            }
            cos_similarities.sort(Comparator.naturalOrder());
            int size = 5;
            if(cos_similarities.size() <5)  size = cos_similarities.size();
            for(int k = 0; k < size;k++){
                new_scores[i] += cos_similarities.get(k);
            }
            new_scores[i] = new_scores[i]/size;
        }

        return new PearsonsCorrelation().correlation(original_scores,new_scores);
    }


    /**
     *
     * @param query
     * @return
     */
    private double get_weighted_information_gain(String query, IndexSearcher searcher){
        // #TODO implement extracting of probalities per document etc.

        return 0d;
    }

    /**
     *
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */
    private double get_normalized_query_commitment(String query, IndexSearcher searcher) throws IOException {
        QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
        Query q = qb.createBooleanQuery("body",query);
        TopDocs top_hits = searcher.search(q,100);
        TopDocs all_hits = searcher.search(q,Integer.MAX_VALUE);
        double mu = 0;
        for(ScoreDoc score_doc:top_hits.scoreDocs){
            mu += score_doc.score;
        }
        mu = mu/top_hits.scoreDocs.length;
        double total_score_sum = 0;
        for(ScoreDoc scoreDoc: all_hits.scoreDocs){
            total_score_sum += scoreDoc.score;
        }
        double tmp_upper_deviation = 0;
        for(ScoreDoc score_doc:top_hits.scoreDocs){
            tmp_upper_deviation += Math.pow((score_doc.score-mu),2);
        }
        tmp_upper_deviation = tmp_upper_deviation/100;
        tmp_upper_deviation = Math.sqrt(tmp_upper_deviation);
        return tmp_upper_deviation/total_score_sum;
    }

    private List<Term> extract_terms(BooleanQuery query){
        List<Term> terms = new ArrayList<>();
        for(BooleanClause clause: query.clauses()){
            terms.add(((TermQuery)clause.getQuery()).getTerm());
        }
        return terms;
    }
}
