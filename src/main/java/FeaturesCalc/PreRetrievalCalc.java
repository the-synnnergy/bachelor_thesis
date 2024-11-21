package FeaturesCalc;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
import weka.core.Attribute;

import java.io.IOException;
import java.util.*;

import static org.apache.commons.math3.stat.StatUtils.populationVariance;
import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;


public class PreRetrievalCalc
{
    /**
     * Class for Prequery Calculations, in this case all the PreQuery Features defined in DOI:10.1145/3078841.
     * As IDF we will use the Lucene Implementation here( i think) #TODO
     * Needs to precalculate and save the following:
     * idf for all Terms, tf for All terms, Collection size, Tf for Collection, Collection Size, overall Termfrequency and
     * the number of tokens in the Collection
     */
    public static class PreretrievalFeatures
    {
        /**
         * Datastructure for holding the Pretrieval Features
         */
        double[] IDFFeatures;
        double[] ICTFFeatures;
        double[] entropyFeatures;
        double[] varFeatures;
        double[] scqFeatures;
        double[] pmiFeatures;
        double queryScope;
        double simplifiedClarityScore;
        double coherenceScore;

        /**
         * Returns a List of Attribute names for weka
         * @return List of Attributesnames
         */
        public static List<Attribute> getWekaAttributesNames()
        {
            List<Attribute> attributes = new ArrayList<>();
            String[] identifiers = new String[]{"query", "target"};
            for (String identifier : identifiers)
            {
                attributes.add(new Attribute("idf_features_avg_" + identifier));
                attributes.add(new Attribute("idf_features_max_" + identifier));
                attributes.add(new Attribute("idf_features_dev_" + identifier));
                attributes.add(new Attribute("ictf_features_avg_" + identifier));
                attributes.add(new Attribute("ictf_features_max_" + identifier));
                attributes.add(new Attribute("ictf_features_dev_" + identifier));
                attributes.add(new Attribute("entropy_features_avg_" + identifier));
                attributes.add(new Attribute("entropy_features_max_" + identifier));
                attributes.add(new Attribute("entropy_features_dev_" + identifier));
                attributes.add(new Attribute("var_features_avg_" + identifier));
                attributes.add(new Attribute("var_features_max_" + identifier));
                attributes.add(new Attribute("var_features_dev_" + identifier));
                attributes.add(new Attribute("scq_features_avg_" + identifier));
                attributes.add(new Attribute("scq_features_max_" + identifier));
                attributes.add(new Attribute("scq_features_dev_" + identifier));
                attributes.add(new Attribute("pmi_features_avg_" + identifier));
                attributes.add(new Attribute("pmi_features_max_" + identifier));
                attributes.add(new Attribute("query_scope_" + identifier));
                attributes.add(new Attribute("simplified_sclarity_score_" + identifier));
                attributes.add(new Attribute("coherence_score_" + identifier));
            }

            return attributes;
        }

        /**
         * Get the Values of the Features as Double List
         * @return List containing values for the features
         */
        public List<Double> getWekaAttributesValues()
        {
            List<Double> values = new ArrayList<>();
            addArrFeaturesToList(values, IDFFeatures, ICTFFeatures, entropyFeatures);
            values.add(entropyFeatures[2]);
            addArrFeaturesToList(values, varFeatures, scqFeatures, pmiFeatures);
            values.add(queryScope);
            values.add(simplifiedClarityScore);
            values.add(coherenceScore);
            return values;
        }


        private void addArrFeaturesToList(List<Double> values, double[] idf_features, double[] ictf_features, double[] entropy_features)
        {
            values.add(idf_features[0]);
            values.add(idf_features[1]);
            values.add(idf_features[2]);
            values.add(ictf_features[0]);
            values.add(ictf_features[1]);
            values.add(ictf_features[2]);
            values.add(entropy_features[0]);
            values.add(entropy_features[1]);
        }


        @Override
        public String toString()
        {
            return (Arrays.toString(IDFFeatures) + " " + Arrays.toString(ICTFFeatures) + " " + Arrays.toString(varFeatures) + " " + Arrays.toString(entropyFeatures) + " " + Arrays.toString(varFeatures)) + " ;queryscope " + queryScope + ";scs:" + simplifiedClarityScore;
        }
    }


    // number of documents in collection
    private final int collectionSize;
    // Term frequency for all Terms. String is the term and long is the freq in the corpus altogether.
    private final Map<String, Long> corpusTermfrequency = new HashMap<>();
    // Maps a term(String) to a Map cotaining the Documents Titles(String) and the freqs(Integer)
    private final Map<String, Map<String, Integer>> termsToDocumentFreqs = new HashMap<>();
    // idf for all terms(String key)
    private final Map<String, Float> IDFMap = new HashMap<>();
    // maps the length for each document(String is title as key)
    private final Map<String, Integer> docLengthMap = new HashMap<>();
    // term prob in corpus mapping
    private final Map<String, Double> scsProbCorpus = new HashMap<>();
    private final IndexReader reader;
    // map for coherence score
    private final HashMap<Integer, HashMap<Integer, Float>> cosSims;
    // IDs of documents containing a given term
    private final HashMap<String, List<Integer>> docIdsContainingTerm;
    private final Analyzer anal;

    /**
     * Constructor which extracts needed data from IndexReader, holds Analyzer and IndexSearcher
     * @param reader Reader on which the features will be calculated
     * @param anal Analyzer for preprocessing, should be the same as the one used in index
     * @throws IOException
     */
    public PreRetrievalCalc(IndexReader reader, Analyzer anal) throws IOException
    {
        this.reader = reader;
        collectionSize = reader.getDocCount("body");
        long totalTokens = reader.getSumTotalTermFreq("body");
        this.anal = anal;
        Terms allTerms = MultiTerms.getTerms(reader, "body");
        TermsEnum allTermsIt = allTerms.iterator();
        // iterates through all terms and fills the maps
        while (allTermsIt.next() != null)
        {
            BytesRef term = allTermsIt.term();
            corpusTermfrequency.put(term.utf8ToString(), allTermsIt.totalTermFreq());
            Map<String, Integer> TFMap = new HashMap<>();
            IDFMap.put(term.utf8ToString(), new ClassicSimilarity().idf(allTermsIt.docFreq(), collectionSize));
            PostingsEnum postings = allTermsIt.postings(null);
            int docId = 0;
            while ((docId = postings.nextDoc()) != NO_MORE_DOCS)
            {
                // Add frequencies to Map key for document length! #TODO fix this to readable!
                docLengthMap.put(reader.document(docId).getField("title").stringValue(), docLengthMap.getOrDefault(reader.document(docId).getField("title").toString(), 0) + postings.freq());
                TFMap.put(reader.document(docId).getField("title").stringValue(), postings.freq());
            }
            // Put map with Frequency for Terms in all Documents in Termmap, so we can find the Termmap via Term and then find frequency via Document title!
            termsToDocumentFreqs.put(term.utf8ToString(), TFMap);
        }

        for (Map.Entry<String, Integer> entry : docLengthMap.entrySet())
        {
            totalTokens += entry.getValue();
        }
        for (Map.Entry<String, Long> entry : corpusTermfrequency.entrySet())
        {
            scsProbCorpus.put(entry.getKey(), ((double) entry.getValue()) / totalTokens);
        }
        // Generate vector space representation

        HashMap<String, Integer> termToVectorindex = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, Long> entry : corpusTermfrequency.entrySet())
        {
            termToVectorindex.put(entry.getKey(), i);
            i++;
        }
        // fill document vectors with 0s and make Map
        // tf-idf term vectors
        Map<String, Double[]> documentTermVectors = new HashMap<>();
        for (int j = 0; j < reader.maxDoc(); j++)
        {
            Double[] termVector = new Double[corpusTermfrequency.size()];
            Arrays.fill(termVector, 0.0);
            documentTermVectors.put(reader.document(j).get("title"), termVector);
        }
        // get terms and change freq in corresponding vector index for all occurences.
        for (Map.Entry<String, Long> entry : corpusTermfrequency.entrySet())
        {
            int index = termToVectorindex.get(entry.getKey());
            Map<String, Integer> tfMap = termsToDocumentFreqs.get(entry.getKey());
            for (Map.Entry<String, Integer> tf_entry : tfMap.entrySet())
            {
                documentTermVectors.get(tf_entry.getKey())[index] = Double.valueOf(tf_entry.getValue()) * IDFMap.get(entry.getKey());
            }
        }
        this.cosSims = precalcQueriesForCoherence();
        this.docIdsContainingTerm = generateTermContainingMap();
    }

    /**
     *  Generates a map which maps Terms as String to a List of Integers, which are docIDs for Index
     * @return
     * @throws IOException
     */
    private HashMap<String, List<Integer>> generateTermContainingMap() throws IOException
    {
        HashMap<String, List<Integer>> docIdsContainingTerm = new HashMap<>();
        Terms allTerms = MultiTerms.getTerms(reader, "body");
        TermsEnum allTermsIt = allTerms.iterator();
        PostingsEnum postings = allTermsIt.postings(null);
        while (allTermsIt.next() != null)
        {
            String term = allTermsIt.term().utf8ToString();
            List<Integer> docsContainingCurrentTerm = new ArrayList<>();
            while (postings.nextDoc() != NO_MORE_DOCS)
            {
                docsContainingCurrentTerm.add(postings.docID());
            }
            docIdsContainingTerm.put(term, docsContainingCurrentTerm);
        }
        return docIdsContainingTerm;
    }

    /**
     * @param query Artifact for which the features should be calculated
     * @return Pretrieval Features wrapped in Datastructure PreretrievalFeatures
     * @throws IOException
     */
    public PreRetrievalCalc.PreretrievalFeatures getPretrievalFeatures(String query) throws IOException
    {
        PreRetrievalCalc.PreretrievalFeatures features = new PreRetrievalCalc.PreretrievalFeatures();
        TokenStream tokenStream = anal.tokenStream("body", query);
        List<String> tokens = new ArrayList<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken())
        {
            tokens.add(attr.toString());
        }
        tokenStream.end();
        tokenStream.close();
        tokens = removeNoncorpusTokens(tokens);
        // #TODO refactor this to filter tokens out first...
        features.IDFFeatures = getIdfFeatures(tokens);
        features.ICTFFeatures = getIctfFeatures(tokens);
        features.entropyFeatures = getEntropyFeatures(tokens);
        features.varFeatures = getVarFeatures(tokens);
        features.scqFeatures = getScqFeatures(tokens);
        features.queryScope = getQueryScope(tokens);
        features.simplifiedClarityScore = getSclarityScore(tokens);
        features.pmiFeatures = getPMIScore(tokens);
        features.coherenceScore = getCoherenceScore(tokens);
        return features;
    }

    /**
     * removes all words from a List that do not appear in the index as terms
     * @param tokens List of string to be trimmed
     * @return stripped List
     */
    private List<String> removeNoncorpusTokens(List<String> tokens)
    {
        ArrayList<String> cleanedTokens = new ArrayList<>();
        for (String token : tokens)
        {
            if (corpusTermfrequency.get(token) != null) cleanedTokens.add(token);
        }
        return cleanedTokens;
    }

    /**
     * Calculates the idf features for the given tokens!
     *
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return Array with idf_features, avg, max and dev! #TODO USE CONSTANTS FOR ACCESSING ARRAY POSITIONS!
     */
    private double[] getIdfFeatures(List<String> tokens)
    {
        double avgIDF = 0;
        double maxIDF = 0;
        int queryTerms = 0;
        Float tmp = null;
        List<Double> termsIDF = new ArrayList<>();
        for (String token : tokens)
        {
            tmp = IDFMap.get(token);
            if (tmp != null)
            {
                termsIDF.add(tmp.doubleValue());
                queryTerms++;
                if (tmp > maxIDF) maxIDF = tmp;
                avgIDF += tmp;
            }
        }
        avgIDF = avgIDF * (1.0d / queryTerms);
        double devIDF = populationVariance(termsIDF.stream().mapToDouble(d -> d).toArray(), avgIDF);
        return new double[]{avgIDF, maxIDF, devIDF};
    }

    /**
     * Calculates the idf Features
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return array containing avgICTF, maxICTF and devICTF
     */
    private double[] getIctfFeatures(List<String> tokens)
    {
        double avgIctf = 0;
        double maxIctf = 0;
        double devIctf;
        int queryTerms = 0;
        List<Double> termsIctf = new ArrayList<>();
        Long tmpTf = null;
        for (String token : tokens)
        {
            tmpTf = corpusTermfrequency.get(token);
            if (tmpTf != null)
            {
                double tmpIctf = Math.log(((double) collectionSize) / tmpTf) / Math.log(2);
                if (tmpIctf > maxIctf) maxIctf = tmpIctf;
                avgIctf += tmpIctf;
                queryTerms++;
                termsIctf.add(tmpIctf);
            }
        }
        avgIctf = avgIctf * (1.0d / queryTerms);
        devIctf = populationVariance(termsIctf.stream().mapToDouble(d -> d).toArray(), avgIctf);
        return new double[]{avgIctf, maxIctf, devIctf};
    }

    /**
     * Calculated the entropy features
     *
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return array with avgEntropy, median entropy and max entropy
     */
    private double[] getEntropyFeatures(List<String> tokens)
    {
        double avgEntropy = 0;
        double medianEntropy;
        //since entropies are negative initializing with 0 was a bad idea, fixed it by setting it to lowest :)
        double maxEntropy = -Double.MAX_VALUE;
        int queryTerms = 0;
        Integer termFrequency = 0;
        List<Double> entropies = new ArrayList<>();
        for (String token : tokens)
        {
            // get the doc freqs
            Map<String, Integer> tfMap = termsToDocumentFreqs.get(token);
            if (tfMap == null) continue;
            queryTerms++;

            double tmpEntropy = 0;
            long corpusTf = corpusTermfrequency.get(token);
            for (Map.Entry<String, Integer> entry : tfMap.entrySet())
            {
                // calculate the entropy
                tmpEntropy += (((double) entry.getValue()) / corpusTf) * (Math.log(((double) entry.getValue()) / corpusTf) / Math.log(collectionSize));
            }
            if (tmpEntropy > maxEntropy) maxEntropy = tmpEntropy;
            entropies.add(tmpEntropy);
            avgEntropy += tmpEntropy;
        }
        avgEntropy = avgEntropy / collectionSize;
        medianEntropy = new Median().evaluate(entropies.stream().mapToDouble(d -> d).toArray());
        return new double[]{avgEntropy, medianEntropy, maxEntropy};
    }

    /**
     * Calculates Variance Features
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return array with average variance, maximum variance and sum of variances.
     */
    private double[] getVarFeatures(List<String> tokens)
    {
        double avgVar = 0;
        double maxVar = 0;
        double sumVar = 0;
        int queryTerms = 0;
        for (String token : tokens)
        {
            Double var = calcVarForTerm(token);
            if (var == null) continue;
            queryTerms++;
            sumVar += var;
            if (var > maxVar) maxVar = var;
        }
        avgVar = sumVar / queryTerms;
        return new double[]{avgVar, maxVar, sumVar};
    }

    /**
     * Calculates the variance for a given term
     * @param token term the variance should be calulcated for
     * @return variance for term
     */
    private Double calcVarForTerm(String token)
    {
        Double var = 0d;
        Map<String, Integer> tfFreqs = termsToDocumentFreqs.get(token);
        if (tfFreqs == null) return null;
        double varUpperSum = 0;
        double[] wtds = new double[tfFreqs.size()];
        int i = 0;
        // calculate the w(t,d) for the term for each document
        for (Map.Entry<String, Integer> entry : tfFreqs.entrySet())
        {
            wtds[i] = (1.0d / (docLengthMap.get(entry.getKey()))) * Math.log(1 + entry.getValue()) * IDFMap.get(token);
            i++;
        }
        // calculate the /w_t for the term
        double avgWtd = (1.0d / collectionSize) * Arrays.stream(wtds).sum();
        // calculate the var for the term
        for (double wtd : wtds)
        {
            varUpperSum += Math.pow(wtd - avgWtd, 2);
        }
        var = Math.sqrt(varUpperSum / wtds.length);
        return var;
    }

    /**
     *Calculates the query scope
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return the query scope
     */
    private double getQueryScope(List<String> tokens)
    {
        // set to track and length of the tf_maps for each term, corpus size is save in collection size
        Set<String> documentsContainingQueryterms = new HashSet<>();
        for (String token : tokens)
        {
            Map<String, Integer> docs = termsToDocumentFreqs.get(token);
            if (docs == null) continue;
            for (Map.Entry<String, Integer> entry : docs.entrySet())
            {
                documentsContainingQueryterms.add(entry.getKey());
            }
        }
        return documentsContainingQueryterms.size() / ((double) collectionSize);
    }

    /**
     * Calculates the simplified clarity score
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return simplified clarity score
     */
    private double getSclarityScore(List<String> tokens)
    {
        Set<String> tokenSet = new HashSet<>(tokens);
        int querySize = tokenSet.size();
        double scs = 0;
        for (String token : tokenSet)
        {
            Double probTokenCorpus = scsProbCorpus.get(token);
            if (probTokenCorpus == null) continue;
            double probTokenQuery = Collections.frequency(tokens, token) / ((double) querySize);
            scs += probTokenQuery * Math.log(probTokenQuery / probTokenCorpus);
        }
        return scs;
    }

    /**
     * Calculates the SCQ Features
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return array with average scq, max scq and sum scq
     */
    private double[] getScqFeatures(List<String> tokens)
    {
        double avgScq = 0;
        double maxScq = 0;
        double sumScq = 0;
        int queryTerms = 0;
        for (String token : tokens)
        {
            Long tf = corpusTermfrequency.get(token);
            Float idf = IDFMap.get(token);
            if (tf == null || idf == null) continue;
            Double scq = (1 + Math.log(tf)) * idf;
            if (scq > maxScq) maxScq = scq;
            sumScq += scq;
            queryTerms++;
        }
        avgScq = sumScq / queryTerms;
        return new double[]{avgScq, maxScq, sumScq};
    }

    /**
     *
     * Calculates the coherence score
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return coherence score
     */
    private double getCoherenceScore(List<String> tokens) throws IOException
    {
        double coherenceScore = 0;
        IndexSearcher searcher = new IndexSearcher(reader);
        for (String token : tokens)
        {
            TermQuery tq = new TermQuery(new Term("body", token));
            TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs());
            searcher.search(tq, collector);
            double tmpCoherenceScore = 0;
            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
            // for each pair of Documents in the top docs
            for (int i = 0; i < scoreDocs.length; i++)
            {
                int doc_id = scoreDocs[i].doc;
                for (int j = i + 1; j < scoreDocs.length; j++)
                {
                    // get the sim between the 2 documents, which was calculated before
                    tmpCoherenceScore += cosSims.get(doc_id).getOrDefault(scoreDocs[j].doc, 0.0f);
                }
            }
            if (tmpCoherenceScore == 0) continue;
            coherenceScore += tmpCoherenceScore / ((double) (scoreDocs.length * (scoreDocs.length - 1)));
        }


        return coherenceScore / ((double) tokens.size());
    }

    /**
     * Precalculates the cos sims for all pairs documents to all other documents and caches it, to save speed
     * @return Map containing Cosine Similarities between the documents of the index
     * @throws IOException
     */
    private HashMap<Integer, HashMap<Integer, Float>> precalcQueriesForCoherence() throws IOException
    {
        IndexSearcher searcher = new IndexSearcher(this.reader);
        HashMap<Integer, HashMap<Integer, Float>> cosSims = new HashMap<>();
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        for (int i = 0; i < reader.numDocs(); i++)
        {
            TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs());
            QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
            Query q = qb.createBooleanQuery("body", reader.document(i).getField("body").stringValue());
            searcher.search(q, collector);
            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
            HashMap<Integer, Float> cosSimsForDoc = new HashMap<>();
            for (ScoreDoc scoreDoc : scoreDocs)
            {
                cosSimsForDoc.put(scoreDoc.doc, scoreDoc.score);
            }
            cosSims.put(i, cosSimsForDoc);
        }
        return cosSims;

    }

    /**
     * Calculates the pmi score
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return array with average pmi score and max pmi score
     */
    private double[] getPMIScore(List<String> tokens)
    {
        double avgPmi = 0.0d;
        double maxPmi = -Double.MAX_VALUE;
        for (int i = 0; i < tokens.size(); i++)
        {
            List<Integer> termA = docIdsContainingTerm.get(tokens.get(i));
            for (int j = i + 1; j < tokens.size(); j++)
            {
                List<Integer> termB = docIdsContainingTerm.get(tokens.get(j));
                // get union and intersect for pmi
                List<Integer> intersect = new ArrayList<>(termB);
                intersect.retainAll(termA);
                double tmpPmi = Math.log((intersect.size() * ((double) reader.numDocs()) / ((double) termA.size() * termB.size())));
                maxPmi = Double.max(maxPmi, tmpPmi);
                avgPmi += tmpPmi;
            }
        }
        return new double[]{avgPmi, maxPmi};
    }

}
