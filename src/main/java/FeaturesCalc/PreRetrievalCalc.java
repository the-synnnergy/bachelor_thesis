package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

/**
 * Class for Prequery Calculations, in this case all the PreQuery Features defined in DOI:10.1145/3078841.
 * As IDF we will use the Lucene Implementation here( i think) #TODO
 * Needs to precalculate and save the following:
 * idf for all Terms, tf for All terms, Collection size, Tf for Collection, Collection Size, overall Termfrequency and
 * the number of tokens in the Collection
 */
public class PreRetrievalCalc
{

    public static class PrequeryFeatures
    {
        double[] idf_features;
        double[] ictf_features;
        double[] entropy_features;
        double[] var_features;
        double[] scq_features;
        double[] pmi_features;
        double query_scope;
        double simplified_clarity_score;
        double coherence_score;

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

        public List<Double> getWekaAttributesValues()
        {
            List<Double> values = new ArrayList<>();
            addArrFeaturesToList(values, idf_features, ictf_features, entropy_features);
            values.add(entropy_features[2]);
            addArrFeaturesToList(values, var_features, scq_features, pmi_features);
            values.add(query_scope);
            values.add(simplified_clarity_score);
            values.add(coherence_score);
            return values;
        }
        // #TODO shitty solution by INtellij lol
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
            return (Arrays.toString(idf_features) + " " + Arrays.toString(ictf_features) + " " + Arrays.toString(var_features) + " " + Arrays.toString(entropy_features) + " " + Arrays.toString(var_features)) + " ;queryscope " + query_scope + ";scs:" + simplified_clarity_score;
        }

        public ArrayList<Pair<String, Double>> to_ArrayList_named(String name)
        {
            ArrayList<Pair<String, Double>> arr_list = new ArrayList<>();
            arr_list.addAll(arr_features_to_arr_list(name));
            arr_list.add(new ImmutablePair<>(name + "query_scope", query_scope));
            arr_list.add(new ImmutablePair<>(name + "simplified_clarity_score", simplified_clarity_score));
            arr_list.add(new ImmutablePair<>(name + "coherence_score", coherence_score));
            return arr_list;
        }

        // bad method, but all other stuff is way more complicated.
        private ArrayList<Pair<String, Double>> arr_features_to_arr_list(String name)
        {
            ArrayList<Pair<String, Double>> arr_list = new ArrayList<>();
            arr_list.add(new ImmutablePair<>(name + "idf_features_avg", idf_features[0]));
            arr_list.add(new ImmutablePair<>(name + "idf_features_max", idf_features[1]));
            arr_list.add(new ImmutablePair<>(name + "idf_features_dev", idf_features[2]));
            arr_list.add(new ImmutablePair<>(name + "ictf_features_avg", ictf_features[0]));
            arr_list.add(new ImmutablePair<>(name + "ictf_features_max", ictf_features[1]));
            arr_list.add(new ImmutablePair<>(name + "ictf_features_dev", ictf_features[2]));
            arr_list.add(new ImmutablePair<>(name + "entropy_features_avg", entropy_features[0]));
            arr_list.add(new ImmutablePair<>(name + "entropy_features_max", entropy_features[1]));
            arr_list.add(new ImmutablePair<>(name + "entropy_features_dev", entropy_features[2]));
            arr_list.add(new ImmutablePair<>(name + "var_features_avg", var_features[0]));
            arr_list.add(new ImmutablePair<>(name + "var_features_max", var_features[1]));
            arr_list.add(new ImmutablePair<>(name + "var_features_dev", var_features[2]));
            arr_list.add(new ImmutablePair<>(name + "scq_features_avg", scq_features[0]));
            arr_list.add(new ImmutablePair<>(name + "scq_features_max", scq_features[1]));
            arr_list.add(new ImmutablePair<>(name + "scq_features_dev", scq_features[2]));
            arr_list.add(new ImmutablePair<>(name + "pmi_features_avg", pmi_features[0]));
            arr_list.add(new ImmutablePair<>(name + "pmi_features_max", pmi_features[1]));
            return arr_list;
        }
    }


    // number of documents in collection
    private final int collection_size;
    // Term frequency for all Terms. String is the term and long is the freq in the corpus altogether.
    private final Map<String, Long> corpus_termfrequency = new HashMap<>();
    // Maps a term(String) to a Map cotaining the Documents Titles(String) and the freqs(Integer)
    private final Map<String, Map<String, Integer>> terms_to_document_freqs = new HashMap<>();
    // idf for all terms(String key)
    private final Map<String, Float> idf_map = new HashMap<>();
    // maps the length for each document(String is title as key)
    private final Map<String, Integer> doc_length_map = new HashMap<>();
    // term prob in corpus mapping
    private final Map<String, Double> scs_prob_corpus = new HashMap<>();
    private final IndexReader reader;
    // tf-idf term vectors
    private final Map<String, Double[]> document_term_vectors = new HashMap<>();
    // map for coherence score
    private final HashMap<Integer, HashMap<Integer, Float>> cos_sims;
    // IDs of documents containing a given term
    private HashMap<String, List<Integer>> docIds_containing_term;
    private final Analyzer anal;

    public PreRetrievalCalc(IndexReader reader, Analyzer anal) throws IOException
    {
        this.reader = reader;
        collection_size = reader.getDocCount("body");
        long total_tokens = reader.getSumTotalTermFreq("body");
        this.anal = anal;
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        while (all_terms_it.next() != null)
        {
            BytesRef term = all_terms_it.term();
            corpus_termfrequency.put(term.utf8ToString(), all_terms_it.totalTermFreq());
            Map<String, Integer> tf_map = new HashMap<>();
            idf_map.put(term.utf8ToString(), new ClassicSimilarity().idf(all_terms_it.docFreq(), collection_size));
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id = 0;
            while ((doc_id = postings.nextDoc()) != NO_MORE_DOCS)
            {
                // Add frequencies to Map key for document length! #TODO fix this to readable!
                doc_length_map.put(reader.document(doc_id).getField("title").stringValue(), doc_length_map.getOrDefault(reader.document(doc_id).getField("title").toString(), 0) + postings.freq());
                tf_map.put(reader.document(doc_id).getField("title").stringValue(), postings.freq());
            }
            // Put map with Frequency for Terms in all Documents in Termmap, so we can find the Termmap via Term and then find frequency via Document title!
            terms_to_document_freqs.put(term.utf8ToString(), tf_map);
        }

        for (Map.Entry<String, Integer> entry : doc_length_map.entrySet())
        {
            total_tokens += entry.getValue();
        }
        for (Map.Entry<String, Long> entry : corpus_termfrequency.entrySet())
        {
            scs_prob_corpus.put(entry.getKey(), ((double) entry.getValue()) / total_tokens);
        }
        // Generate vector space representation

        HashMap<String, Integer> term_to_vectorindex = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, Long> entry : corpus_termfrequency.entrySet())
        {
            term_to_vectorindex.put(entry.getKey(), i);
            i++;
        }
        // fill document vectors with 0s and make Map
        for (int j = 0; j < reader.maxDoc(); j++)
        {
            Double[] term_vector = new Double[corpus_termfrequency.size()];
            Arrays.fill(term_vector, 0.0);
            document_term_vectors.put(reader.document(j).get("title"), term_vector);
        }
        // get terms and change freq in corresponding vector index for all occurences.
        for (Map.Entry<String, Long> entry : corpus_termfrequency.entrySet())
        {
            int index = term_to_vectorindex.get(entry.getKey());
            Map<String, Integer> tf_map = terms_to_document_freqs.get(entry.getKey());
            for (Map.Entry<String, Integer> tf_entry : tf_map.entrySet())
            {
                document_term_vectors.get(tf_entry.getKey())[index] = Double.valueOf(tf_entry.getValue()) * idf_map.get(entry.getKey());
            }
        }
        this.cos_sims = precalcQueriesForPMI();
        this.docIds_containing_term = generateTermContainingMap();
    }

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
     * @param query
     * @return
     * @throws IOException
     */
    public PrequeryFeatures get_prequery_features(String query) throws IOException
    {
        PrequeryFeatures features = new PrequeryFeatures();
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
        tokens = remove_noncorpus_tokens(tokens);
        // #TODO refactor this to filter tokens out first...
        features.idf_features = get_idf_features(tokens);
        features.ictf_features = get_ictf_features(tokens);
        features.entropy_features = get_entropy_features(tokens);
        features.var_features = get_var_features(tokens);
        features.scq_features = get_scq_features(tokens);
        features.query_scope = get_query_scope(tokens);
        features.simplified_clarity_score = get_sclarity_score(tokens);
        features.pmi_features = getPMIScore(tokens);
        features.coherence_score = getCoherenceScore(tokens);
        return features;
    }

    /**
     * @param tokens
     * @return
     */
    private List<String> remove_noncorpus_tokens(List<String> tokens)
    {
        ArrayList<String> cleaned_tokens = new ArrayList<>();
        for (String token : tokens)
        {
            if (corpus_termfrequency.get(token) != null) cleaned_tokens.add(token);
        }
        return cleaned_tokens;
    }

    /**
     * Calculates the idf features for the given tokens!
     *
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return Array with idf_features, avg, max and dev! #TODO USE CONSTANTS FOR ACCESSING ARRAY POSITIONS!
     */
    private double[] get_idf_features(List<String> tokens)
    {
        double avg_idf = 0;
        double max_idf = 0;
        int query_terms = 0;
        Float tmp = null;
        List<Double> terms_idf = new ArrayList<>();
        for (String token : tokens)
        {
            tmp = idf_map.get(token);
            if (tmp != null)
            {
                terms_idf.add(tmp.doubleValue());
                query_terms++;
                if (tmp > max_idf) max_idf = tmp;
                avg_idf += tmp;
            }
        }
        avg_idf = avg_idf * (1.0d / query_terms);
        double dev_idf = populationVariance(terms_idf.stream().mapToDouble(d -> d).toArray(), avg_idf);
        return new double[]{avg_idf, max_idf, dev_idf};
    }

    /**
     * @param tokens
     * @return
     */
    private double[] get_ictf_features(List<String> tokens)
    {
        double avg_ictf = 0;
        double max_ictf = 0;
        double dev_ictf;
        int query_terms = 0;
        List<Double> terms_ictf = new ArrayList<>();
        Long tmp_tf = null;
        for (String token : tokens)
        {
            tmp_tf = corpus_termfrequency.get(token);
            if (tmp_tf != null)
            {
                double tmp_ictf = Math.log(((double) collection_size) / tmp_tf) / Math.log(2);
                if (tmp_ictf > max_ictf) max_ictf = tmp_ictf;
                avg_ictf += tmp_ictf;
                query_terms++;
                terms_ictf.add(tmp_ictf);
            }
        }
        avg_ictf = avg_ictf * (1.0d / query_terms);
        dev_ictf = populationVariance(terms_ictf.stream().mapToDouble(d -> d).toArray(), avg_ictf);
        return new double[]{avg_ictf, max_ictf, dev_ictf};
    }

    /**
     * #TODO Some error here in calculation, should be fixed
     *
     * @param tokens
     * @return
     */
    private double[] get_entropy_features(List<String> tokens)
    {
        double avg_entropy = 0;
        double median_entropy;
        //since entropies are negative initializing with 0 was a bad idea, fixed it by setting it to lowest :)
        double max_entropy = -Double.MAX_VALUE;
        int query_terms = 0;
        Integer term_frequency = 0;
        List<Double> entropies = new ArrayList<>();
        for (String token : tokens)
        {
            Map<String, Integer> tf_map = terms_to_document_freqs.get(token);
            if (tf_map == null) continue;
            query_terms++;

            double tmp_entropy = 0;
            long corpus_tf = corpus_termfrequency.get(token);
            for (Map.Entry<String, Integer> entry : tf_map.entrySet())
            {
                tmp_entropy += (((double) entry.getValue()) / corpus_tf) * (Math.log(((double) entry.getValue()) / corpus_tf) / Math.log(collection_size));
            }
            if (tmp_entropy > max_entropy) max_entropy = tmp_entropy;
            entropies.add(tmp_entropy);
            avg_entropy += tmp_entropy;
        }
        avg_entropy = avg_entropy / collection_size;
        median_entropy = new Median().evaluate(entropies.stream().mapToDouble(d -> d).toArray());
        return new double[]{avg_entropy, median_entropy, max_entropy};
    }

    /**
     * @param tokens
     * @return
     */
    private double[] get_var_features(List<String> tokens)
    {
        double avg_var = 0;
        double max_var = 0;
        double sum_var = 0;
        int query_terms = 0;
        for (String token : tokens)
        {
            Double var = calc_var_for_term(token);
            if (var == null) continue;
            query_terms++;
            sum_var += var;
            if (var > max_var) max_var = var;
        }
        avg_var = sum_var / query_terms;
        return new double[]{avg_var, max_var, sum_var};
    }

    /**
     * @param token
     * @return
     */
    private Double calc_var_for_term(String token)
    {
        Double var = 0d;
        Map<String, Integer> tf_freqs = terms_to_document_freqs.get(token);
        if (tf_freqs == null) return null;
        double var_upper_sum = 0;
        double[] wtds = new double[terms_to_document_freqs.size()];
        int i = 0;
        for (Map.Entry<String, Integer> entry : tf_freqs.entrySet())
        {
            wtds[i] = (1.0d / (doc_length_map.get(entry.getKey()))) * Math.log(1 + entry.getValue()) * idf_map.get(token);
        }
        double avg_wtd = (1.0d / collection_size) * Arrays.stream(wtds).sum();
        for (double wtd : wtds)
        {
            var_upper_sum += Math.pow(wtd - avg_wtd, 2);
        }
        var = Math.sqrt(var_upper_sum / wtds.length);
        return var;
    }

    /**
     * @param tokens
     * @return
     */
    private double get_query_scope(List<String> tokens)
    {
        // set to track and length of the tf_maps for each term, corpus size is save in collection size
        Set<String> documents_containing_queryterms = new HashSet<>();
        for (String token : tokens)
        {
            Map<String, Integer> docs = terms_to_document_freqs.get(token);
            if (docs == null) continue;
            for (Map.Entry<String, Integer> entry : docs.entrySet())
            {
                documents_containing_queryterms.add(entry.getKey());
            }
        }
        return documents_containing_queryterms.size() / ((double) collection_size);
    }

    /**
     * #TODO seems to have bugs...
     *
     * @param tokens
     * @return
     */
    private double get_sclarity_score(List<String> tokens)
    {
        Set<String> token_set = new HashSet<>(tokens);
        int query_size = token_set.size();
        double scs = 0;
        for (String token : token_set)
        {
            Double prob_token_corpus = scs_prob_corpus.get(token);
            if (prob_token_corpus == null) continue;
            double prob_token_query = Collections.frequency(tokens, token) / ((double) query_size);
            scs += prob_token_query * Math.log(prob_token_query / prob_token_corpus);
        }
        return scs;
    }

    /**
     * @param tokens
     * @return
     */
    private double[] get_scq_features(List<String> tokens)
    {
        double avg_scq = 0;
        double max_scq = 0;
        double sum_scq = 0;
        int query_terms = 0;
        for (String token : tokens)
        {
            Long tf = corpus_termfrequency.get(token);
            Float idf = idf_map.get(token);
            if (tf == null || idf == null) continue;
            Double scq = (1 + Math.log(tf)) * idf;
            if (scq > max_scq) max_scq = scq;
            sum_scq += scq;
            query_terms++;
        }
        avg_scq = sum_scq / query_terms;
        return new double[]{avg_scq, max_scq, sum_scq};
    }

    /**
     * #TODO needs to use stripped tokens
     *
     * @param tokens
     * @return
     */
    private double getCoherenceScore(List<String> tokens) throws IOException
    {
        double coherence_score = 0;
        IndexSearcher searcher = new IndexSearcher(reader);
        for (String token : tokens)
        {
            // get documents containing query token
            // # TODO do this in get_prequery_features!!!! the queries need to be done in constructor and then saved to a map!
            TermQuery tq = new TermQuery(new Term("body", token));
            // apache lucene giving error when set to Integer.MAX_VALUE, thus its set to 2147483630
            TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs());
            searcher.search(tq, collector);
            double tmp_coherence_score = 0;
            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
            for (int i = 0; i < scoreDocs.length; i++)
            {
                int doc_id = scoreDocs[i].doc;
                for (int j = i + 1; j < scoreDocs.length; j++)
                {
                    tmp_coherence_score += cos_sims.get(doc_id).getOrDefault(scoreDocs[j].doc, 0.0f);
                }
            }
            if(tmp_coherence_score == 0) continue;
            coherence_score += tmp_coherence_score / ((double) (scoreDocs.length * (scoreDocs.length - 1)));
        }


        return coherence_score / ((double) tokens.size());
    }

    private HashMap<Integer, HashMap<Integer, Float>> precalcQueriesForPMI() throws IOException
    {
        IndexSearcher searcher = new IndexSearcher(this.reader);
        HashMap<Integer, HashMap<Integer, Float>> cos_sims = new HashMap<>();
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        for (int i = 0; i < reader.numDocs(); i++)
        {
            TopScoreDocCollector collector = TopScoreDocCollector.create(reader.numDocs(), reader.numDocs());
            QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
            Query q = qb.createBooleanQuery("body", reader.document(i).getField("body").stringValue());
            searcher.search(q, collector);
            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
            HashMap<Integer, Float> cos_sims_for_doc = new HashMap<>();
            for (ScoreDoc scoreDoc : scoreDocs)
            {
                cos_sims_for_doc.put(scoreDoc.doc, scoreDoc.score);
            }
            cos_sims.put(i, cos_sims_for_doc);
        }
        return cos_sims;

    }


    private double[] getPMIScore(List<String> tokens)
    {
        double avg_pmi = 0.0d;
        double max_pmi = -Double.MAX_VALUE;
        for (int i = 0; i < tokens.size(); i++)
        {
            List<Integer> term_a = docIds_containing_term.get(tokens.get(i));
            for (int j = i + 1; j < tokens.size(); j++)
            {
                List<Integer> term_b = docIds_containing_term.get(tokens.get(j));
                List<Integer> intersect = new ArrayList<>(term_b);
                intersect.retainAll(term_a);
                double tmpPmi = Math.log((intersect.size() * ((double) reader.numDocs()) / ((double) term_a.size() * term_b.size())));
                max_pmi = Double.max(max_pmi, tmpPmi);
                avg_pmi += tmpPmi;
            }
        }
        return new double[]{avg_pmi, max_pmi};
    }

}
