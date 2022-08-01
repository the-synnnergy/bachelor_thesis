package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.QueryBuilder;
import weka.core.Attribute;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static FeaturesCalc.Util.*;

/**
 * Class for calculating the Postquery features mentioned in C. Mills. #TODO add reference
 */
public class PostRetrievalCalc
{
    /**
     * Class for storing the features, so they can be returned and then easily extracted.
     */
    public static class PostQueryFeatures
    {
        double subqueryOverlap;
        double robustnessScore;
        double firstRankChange;
        double clusteringTendency;
        double spatialAutocorrelation;
        double weightedInformationGain;
        double normalizedQueryCommitment;

        public PostQueryFeatures()
        { /* TODO document why this constructor is empty */ }

        public void print()
        {
            System.out.println("subquery_overlap:" + subqueryOverlap);
            System.out.println("robustness_score:" + robustnessScore);
            System.out.println("first_rank_change:" + firstRankChange);
            System.out.println("clustering_tendency:" + clusteringTendency);
            System.out.println("spatial_autocorreleation:" + spatialAutocorrelation);
            System.out.println("WIG:" + weightedInformationGain);
            System.out.println("Normalized query commitment:" + normalizedQueryCommitment);
        }

        /**
         * Return array list with tuples containing feature name and value
         *
         * @param name name prefix for feature
         * @return array list with tuples containing feature name with given prefix name and values
         */
        public List<Pair<String, Double>> toArrayListNamed(String name)
        {
            ArrayList<Pair<String, Double>> arrayList = new ArrayList<>();
            arrayList.add(new ImmutablePair<>(name + "subquery_overlap", subqueryOverlap));
            arrayList.add(new ImmutablePair<>(name + "robustness_score", robustnessScore));
            arrayList.add(new ImmutablePair<>(name + "first_rank_change", firstRankChange));
            arrayList.add(new ImmutablePair<>(name + "clustering_tendency", clusteringTendency));
            arrayList.add(new ImmutablePair<>(name + "spatial_autocorrelation", spatialAutocorrelation));
            arrayList.add(new ImmutablePair<>(name + "weighted_information_gain", weightedInformationGain));
            arrayList.add(new ImmutablePair<>(name + "normalized_query_commitment", normalizedQueryCommitment));
            return arrayList;
        }

        public static List<Attribute> getWekaAttributesNames()
        {
            List<Attribute> attributes = new ArrayList<>();
            for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
            {
                String query =  "query_";
                attributes.add(new Attribute("subquery_overlap_" + query + sim.ordinal()));
                attributes.add(new Attribute("robustness_score_" + query + sim.ordinal()));
                attributes.add(new Attribute("first_rank_change_" + query + sim.ordinal()));
                attributes.add(new Attribute("clustering_tendency_" + query + sim.ordinal()));
                attributes.add(new Attribute("spatial_autocorrelation_" + query + sim.ordinal()));
                attributes.add(new Attribute("weighted_information_gain_" + query + sim.ordinal()));
                attributes.add(new Attribute("normalized_query_commitment_" + query + sim.ordinal()));
            }
            for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
            {
                String target_ = "target_";

                attributes.add(new Attribute("subquery_overlap_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("robustness_score_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("first_rank_change_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("clustering_tendency_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("spatial_autocorrelation_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("weighted_information_gain_" + target_ + sim.ordinal()));
                attributes.add(new Attribute("normalized_query_commitment_" + target_ + sim.ordinal()));
            }


            return attributes;
        }

        public List<Double> getWekaAttributesValues()
        {
            List<Double> values = new ArrayList<>();
            values.add(subqueryOverlap);
            values.add(robustnessScore);
            values.add(firstRankChange);
            values.add(clusteringTendency);
            values.add(spatialAutocorrelation);
            values.add(weightedInformationGain);
            values.add(normalizedQueryCommitment);
            return values;
        }


    }

    IndexReader reader;
    IndexSearcher searcher;
    Map<Integer, double[]> documentToTermvectors;
    private static final int MAX_HITS_SUBQUERY = 10;
    private static final int MAX_HITS_ROBUSTNESS_SCORE = 50;
    private static final int MAX_HITS_WEIGHTED_INFORMATION_GAIN = 100;
    private static final int MAX_HITS_CLUSTERING = 100;
    private Analyzer anal;

    /***
     * Constructor for PostqueryCalc with given reader and analyzer
     * @param reader IndexReader where the Postquery features should be calculcated on
     * @param anal Analyzer which should be used for Stemming and stopword removal, must be the same(language, stopwords, stemming rules) as used in Index!
     * @throws IOException -
     */
    public PostRetrievalCalc(IndexReader reader, Analyzer anal, Similarity sim) throws IOException, IllegalArgumentException
    {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        //if (reader.numDocs() < 100) throw new IllegalArgumentException("atleast 100 documents needed for useful data!");
        this.reader = reader;
        this.searcher = new IndexSearcher(reader);
        searcher.setSimilarity(sim);
        documentToTermvectors = Util.get_idf_document_vectors(reader);
        this.anal = anal;
    }

    /***
     * return an object containing Postquery features values for a given query
     * @param query the string for which the features should be calculated
     * @return PostqueryFeatures object which contains all feature values
     * @throws IOException -
     */
    public PostQueryFeatures getPostQueryFeatures(String query) throws IOException
    {
        this.anal = new EnglishAnalyzer();
        PostQueryFeatures features = new PostQueryFeatures();
        features.clusteringTendency = getClusteringTendency(query, searcher);
        features.firstRankChange = getFirstRankChange(query, searcher);
        features.normalizedQueryCommitment = getNormalizedQueryCommitment(query, searcher);
        features.robustnessScore = getRobustnessScore(query, searcher);
        features.spatialAutocorrelation = getSpatialAutocorrelation(query, searcher);
        features.subqueryOverlap = getSubqueryOverlap(query, searcher);
        features.weightedInformationGain = getWeightedInformationGain(query, searcher);
        return features;
    }

    /**
     * Method to calculated subquery overlap
     *
     * @param query    String which is to be queried and for which subquery overlap is calculated, preprocessed with analyzer
     * @param searcher IndexSearcher on the given IndexReader in the constructor
     * @return subquery overlap as double
     * @throws IOException -
     */

    private double getSubqueryOverlap(String query, IndexSearcher searcher) throws IOException
    {
        // get first then hits from query.
        QueryBuilder qb = new QueryBuilder(anal);
        Query booleanQuery = qb.createBooleanQuery("body", query);
        TopDocs initialResult = searcher.search(booleanQuery, MAX_HITS_SUBQUERY);
        List<Integer> initialIndices = new ArrayList<>();
        for (ScoreDoc hit : initialResult.scoreDocs)
        {
            initialIndices.add(hit.doc);
        }
        List<Double> overlappingResults = new ArrayList<>();
        for (BooleanClause clause : ((BooleanQuery) booleanQuery).clauses())
        {
            double overlappingResultsTmp = 0;
            TopDocs tmpResults = searcher.search(clause.getQuery(), MAX_HITS_SUBQUERY);
            for (ScoreDoc hit : tmpResults.scoreDocs)
            {
                if (initialIndices.contains(hit.doc)) overlappingResultsTmp++;
            }
            overlappingResults.add(overlappingResultsTmp);
        }
        return new StandardDeviation().evaluate(overlappingResults.stream().mapToDouble(Double::doubleValue).toArray());
    }

    /**
     * Calculates the robustness score for given query
     *
     * @param query    String which is to be queried and for which robustness score is calculated, preprocessed with analyzer
     * @param searcher IndexSearcher on the given IndexReader in the constructor
     * @return robustness score as double
     * @throws IOException
     */
    private double getRobustnessScore(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(anal);
        Query booleanQuery = qb.createBooleanQuery("body", query);
        TopDocs initialResult = searcher.search(booleanQuery, MAX_HITS_ROBUSTNESS_SCORE);
        List<Integer> rankedList = new ArrayList<>();
        for (ScoreDoc hit : initialResult.scoreDocs)
        {
            rankedList.add(hit.doc);
        }
        List<Term> termList = extractTermsBooleanQuery((BooleanQuery) booleanQuery);
        // construct pertubed query
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.setMinimumNumberShouldMatch(1);
        for (Term term : termList)
        {
            queryBuilder.add(new PertubedQuery(term, rankedList), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubedQuery = queryBuilder.build();
        List<Double> spearmanRankCorrelations = new ArrayList<>();
        for (int i = 0; i < 100; i++)
        {
            TopDocs result = searcher.search(pertubedQuery, MAX_HITS_ROBUSTNESS_SCORE);
            List<Integer> pertubedRankedList = new ArrayList<>();
            for (ScoreDoc hit : result.scoreDocs)
            {
                pertubedRankedList.add(hit.doc);
            }
            double tmpSpearman = new SpearmansCorrelation().correlation(rankedList.stream().mapToDouble(d -> d).toArray(), pertubedRankedList.stream().mapToDouble(d -> d).toArray());
            spearmanRankCorrelations.add(tmpSpearman);
        }
        return spearmanRankCorrelations.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /***
     * Calculcates the first rank change for the given query
     * @param query String which is to be queried, only preprocessed with analyzer
     * @param searcher IndexSearcher on IndexReader used in constructor
     * @return first rank change as double
     * @throws IOException
     */
    private double getFirstRankChange(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(anal);
        Query booleanQuery = qb.createBooleanQuery("body", query);
        TopDocs initialResult = searcher.search(booleanQuery, MAX_HITS_ROBUSTNESS_SCORE);
        int topHit = initialResult.scoreDocs[0].doc;
        List<Integer> rankedList = new ArrayList<>();
        for (ScoreDoc hit : initialResult.scoreDocs)
        {
            rankedList.add(hit.doc);
        }
        List<Term> termList = extractTermsBooleanQuery((BooleanQuery) booleanQuery);
        // construct pertubed query
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.setMinimumNumberShouldMatch(1);
        for (Term term : termList)
        {
            queryBuilder.add(new PertubedQuery(term, rankedList), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery pertubedQuery = queryBuilder.build();
        int firstRankChangeSum = 0;
        for (int i = 0; i < 100; i++)
        {
            TopDocs result = searcher.search(pertubedQuery, MAX_HITS_ROBUSTNESS_SCORE);
            if (result.scoreDocs[0].doc == topHit) firstRankChangeSum++;
        }
        return firstRankChangeSum;
    }

    /**
     * Calculates the clustering tendency for a given query
     *
     * @param query    String for which clustering tendency is calculated, needs to be preprocessed
     * @param searcher IndexSearcher on IndexReader used in Constructor
     * @return Clustering tendency as double
     * @throws IOException
     */
    private double getClusteringTendency(String query, IndexSearcher searcher) throws IOException
    {
        if(reader.numDocs() < 100) return Double.NaN;
        QueryBuilder qb = new QueryBuilder(anal);
        Query booleanQuery = qb.createBooleanQuery("body", query);
        TopDocs top100 = searcher.search(booleanQuery, MAX_HITS_CLUSTERING);
        Set<Integer> docIds = new HashSet<>();
        for (ScoreDoc top_doc : top100.scoreDocs)
        {
            docIds.add(top_doc.doc);
        }
        Set<Integer> sampleablePoints = new HashSet<>();
        TopDocs allDocs = searcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
        for (ScoreDoc scoredoc : allDocs.scoreDocs)
        {
            sampleablePoints.add(scoredoc.doc);
        }
        // Now its time to remove all Docs already in the top 100 from sampling!
        sampleablePoints.removeAll(docIds);
        if(sampleablePoints.size() == 0)
        {
            return Double.NaN;
        }
        Map<Integer, double[]> termVectors = get_idf_document_vectors(reader);
        double mean = 0;
        for (int i = 0; i < 100; i++)
        {
            mean += getSimQueryForMean(sampleablePoints.toArray(new Integer[0]), searcher, docIds, top100, termVectors, query);
        }
        mean = mean / 100;
        int termvectorLength = termVectors.get(0).length;
        double[] max = new double[termvectorLength];
        double[] min = new double[termvectorLength];
        Arrays.fill(min, Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : top100.scoreDocs)
        {
            double[] termvector = termVectors.get(scoreDoc.doc);
            for (int i = 0; i < termvectorLength; i++)
            {
                if (termvector[i] > max[i]) max[i] = termvector[i];
                if (termvector[i] < min[i]) min[i] = termvector[i];
            }
        }
        double sum = 0;
        for (int i = 0; i < termvectorLength; i++)
        {
            sum += max[i] - min[i];
        }
        return mean * (1.0d / termvectorLength) * sum;
    }

    /**
     * Submethod for calculating the Sim_query from Mill paper which is needed for Clustering Tendency
     *
     * @param sampleablePoints Document ids which are not in the top 100 results from query
     * @param searcher          IndexSearcher on IndexReader used in Constructor
     * @param docIds           Top100 query document ids
     * @param top100            TopDocs result from query
     * @param termVectors      idf vectors for index
     * @param query             query string
     * @return sim query for one samples point
     * @throws IOException
     */
    private double getSimQueryForMean(Integer[] sampleablePoints, IndexSearcher searcher, Set<Integer> docIds, TopDocs top100, Map<Integer, double[]> termVectors, String query) throws IOException
    {
        int sampleablePoint = sampleablePoints[ThreadLocalRandom.current().nextInt(sampleablePoints.length)];
        QueryBuilder qb = new QueryBuilder(anal);
        Query sampledPointQuery = qb.createBooleanQuery("body", (reader.document(sampleablePoint).get("body")));
        TopDocs resultDocs = searcher.search(sampledPointQuery, Integer.MAX_VALUE);
        int markedPoint = 0;
        for (ScoreDoc result : resultDocs.scoreDocs)
        {
            if (docIds.contains(result.doc))
            {
                markedPoint = result.doc;
                break;
            }
        }
        int markedPointIndex = 0;
        for (int i = 0; i < top100.scoreDocs.length; i++)
        {
            if (top100.scoreDocs[i].doc == markedPoint)
            {
                markedPointIndex = i;
                break;
            }
        }
        int nearestNeighbor = 0;
        if (markedPointIndex == 0) nearestNeighbor = 1;
        if (markedPointIndex == top100.scoreDocs.length - 1) nearestNeighbor = markedPointIndex - 1;
        if (markedPointIndex != 0 && markedPointIndex != top100.scoreDocs.length - 1)
        {
            if (top100.scoreDocs[markedPointIndex - 1].score > (top100.scoreDocs[markedPointIndex + 1].score))
            {
                nearestNeighbor = markedPointIndex - 1;
            } else
            {
                nearestNeighbor = markedPointIndex + 1;
            }
        }
        int nearestNeighborDocId = top100.scoreDocs[nearestNeighbor].doc;
        int markedPointDocId = top100.scoreDocs[markedPointIndex].doc;
        double simQueryDmpDnn = calcSimQuery(termVectors.get(markedPointDocId), termVectors.get(nearestNeighborDocId), get_query_idf_termvector(query, reader));
        double simQueryPspDmp = calcSimQuery(termVectors.get(sampleablePoint), termVectors.get(markedPointDocId), get_query_idf_termvector(query, reader));

        return simQueryDmpDnn / simQueryPspDmp;
    }

    /**
     * @param first
     * @param second
     * @param query_termvector
     * @return
     */
    private double calcSimQuery(double[] first, double[] second, double[] query_termvector)
    {
        assert query_termvector.length == first.length;
        assert first.length == second.length;
        double simQueryUpperPart1 = 0;
        double simQueryLowerPart1Sum1 = 0;
        double simQueryLowerPart1Sum2 = 0;
        double simQueryUpperPart2 = 0;
        double simQueryLowerPart2Sum1 = 0;
        double simQueryLowerPart2Sum2 = 0;
        for (int i = 0; i < first.length; i++)
        {
            double firstFreq = first[i];
            double secondFreq = second[i];
            simQueryUpperPart1 += firstFreq * secondFreq;
            simQueryLowerPart1Sum1 += Math.pow(firstFreq, 2);
            simQueryLowerPart1Sum2 += Math.pow(secondFreq, 2);
            simQueryUpperPart2 += ((firstFreq + secondFreq) / 2) * query_termvector[i];
            simQueryLowerPart2Sum1 += Math.pow(((firstFreq + secondFreq) / 2), 2);
            simQueryLowerPart2Sum2 += Math.pow(query_termvector[i], 2);
        }
        if(simQueryLowerPart1Sum1 * simQueryLowerPart1Sum2 == 0) throw new ArithmeticException("calcSimquery denominator is zero");
        double simQueryPart1 = simQueryUpperPart1 / (simQueryLowerPart1Sum1 * simQueryLowerPart1Sum2);
        double simQueryPart2 = simQueryUpperPart2 / (simQueryLowerPart2Sum1 * simQueryLowerPart2Sum2);
        return simQueryPart1 * simQueryPart2;
    }

    /**
     * Method to calculated spatial autocorrelation for given query
     *
     * @param query    String for which the spatial autocorrelation is calculated
     * @param searcher IndexSearcher on IndexReader used in constructor
     * @return spatial autocorrelation as double
     * @throws IOException
     */
    private double getSpatialAutocorrelation(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(anal);
        Query booleanQuery = qb.createBooleanQuery("body", query);
        TopDocs hits = searcher.search(booleanQuery, 50);
        double[] originalScores = new double[hits.scoreDocs.length];
        for (int i = 0; i < hits.scoreDocs.length; i++)
        {
            originalScores[i] = hits.scoreDocs[i].score;
        }
        double[] newScores = new double[hits.scoreDocs.length];
        for (int i = 0; i < hits.scoreDocs.length; i++)
        {
            List<Double> cosSimilarities = new ArrayList<>();
            // Calculate cosine sim for each
            double[] termvectorI = documentToTermvectors.get(hits.scoreDocs[i].doc);
            for (int j = 0; j < hits.scoreDocs.length; j++)
            {
                if (j == i) continue;

                double[] termvectorJ = documentToTermvectors.get(hits.scoreDocs[j].doc);
                double cosSim = Util.cos_sim(termvectorI, termvectorJ);
                cosSimilarities.add(cosSim);
            }
            cosSimilarities.sort(Comparator.naturalOrder());
            int size = Math.min(cosSimilarities.size(), 5);
            for (int k = 0; k < size; k++)
            {
                newScores[i] += cosSimilarities.get(k);
            }
            newScores[i] = newScores[i] / size;
        }

        return new PearsonsCorrelation().correlation(originalScores, newScores);
    }


    /**
     * Calculated the weighted information for given query string
     *
     * @param query    String for which weighted information is calculated
     * @param searcher
     * @return
     */

    private double getWeightedInformationGain(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(anal);
        Query q = qb.createBooleanQuery("body", query);
        TopDocs topHits = searcher.search(q, MAX_HITS_WEIGHTED_INFORMATION_GAIN);
        List<String> queryTerms = get_query_terms(query, reader, anal);
        double lambda = 1 / Math.sqrt(queryTerms.size());
        double weightedInformationGain = 0.0d;
        Map<String, Double> corpusProbs = get_corpus_probs(reader);
        Map<Integer, Map<String, Double>> perDocumentProbs = Util.get_document_probs(reader);
        for (ScoreDoc scoreDoc : topHits.scoreDocs)
        {
            Map<String, Double> termProbabilitiesDocument = perDocumentProbs.get(scoreDoc.doc);
            for (String term : queryTerms)
            {
                if (corpusProbs.getOrDefault(term, 0.0d) == 0.0d) continue;
                if (termProbabilitiesDocument.getOrDefault(term, 0.0d) == 0.0d) continue;
                weightedInformationGain += lambda * Math.log(termProbabilitiesDocument.getOrDefault(term, 1.0d) / corpusProbs.getOrDefault(term, 1.0d));
            }
        }
        return weightedInformationGain / topHits.scoreDocs.length;
    }

    /**
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */

    private double getNormalizedQueryCommitment(String query, IndexSearcher searcher) throws IOException
    {
        QueryBuilder qb = new QueryBuilder(anal);
        Query q = qb.createBooleanQuery("body", query);
        TopDocs topHits = searcher.search(q, 100);
        TopDocs allHits = searcher.search(q, Integer.MAX_VALUE);
        double mu = 0;
        for (ScoreDoc score_doc : topHits.scoreDocs)
        {
            mu += score_doc.score;
        }
        mu = mu / topHits.scoreDocs.length;
        double totalScoreSum = 0;
        for (ScoreDoc scoreDoc : allHits.scoreDocs)
        {
            totalScoreSum += scoreDoc.score;
        }
        double tmpUpperDeviation = 0;
        for (ScoreDoc score_doc : topHits.scoreDocs)
        {
            tmpUpperDeviation += Math.pow((score_doc.score - mu), 2);
        }
        tmpUpperDeviation = tmpUpperDeviation / 100;
        tmpUpperDeviation = Math.sqrt(tmpUpperDeviation);
        return tmpUpperDeviation / totalScoreSum;
    }

    private List<Term> extractTermsBooleanQuery(BooleanQuery query)
    {
        List<Term> terms = new ArrayList<>();
        for (BooleanClause clause : query.clauses())
        {
            terms.add(((TermQuery) clause.getQuery()).getTerm());
        }
        return terms;
    }


}

