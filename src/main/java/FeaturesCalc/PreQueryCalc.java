package FeaturesCalc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.MathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class PreQueryCalc
{

    public class PrequeryFeatures
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

        @Override
        public String toString()
        {
            return (Arrays.toString(idf_features) + " " + Arrays.toString(ictf_features) + " " + Arrays.toString(var_features) + " " + Arrays.toString(entropy_features) + " " + Arrays.toString(var_features)) + " ;queryscope " + query_scope + ";scs:" + simplified_clarity_score;
        }
    }

    private List<ImmutablePair<String, String>> corpus;
    // number of documents in collection
    private final int collection_size;
    // Term frequency for all Terms. String is the term and long is the freq in the corpus altogether.
    private final Map<String, Long> corpus_termfrequency = new HashMap<>();
    // Maps a term(String) to a Map cotaining the Documents Titles(String) and the freqs(Integer)
    private final Map<String, Map<String, Integer>> map_to_termfrequency_map = new HashMap<>();
    // idf for all terms(String key)
    private final Map<String, Float> idf_map = new HashMap<>();
    // maps the length for each document(String is title as key)
    private final Map<String, Integer> doc_length_map = new HashMap<>();
    private long total_tokens;
    // term prob in corpus mapping
    private final Map<String, Double> scs_prob_corpus = new HashMap<>();
    private final IndexReader reader;
    // tf-idf term vectors
    private final Map<String, Double[]> document_term_vectors = new HashMap<>();

    public PreQueryCalc(IndexReader reader) throws IOException
    {
        this.reader = reader;
        collection_size = reader.getDocCount("body");
        total_tokens = reader.getSumTotalTermFreq("body");
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
                doc_length_map.put(reader.document(doc_id).getField("title").toString(), doc_length_map.getOrDefault(reader.document(doc_id).getField("title").toString(), 0) + postings.freq());
                tf_map.put(reader.document(doc_id).getField("title").toString(), postings.freq());
            }
            // Put map with Frequency for Terms in all Documents in Termmap, so we can find the Termmap via Term and then find frequency via Document title!
            map_to_termfrequency_map.put(term.utf8ToString(), tf_map);
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
        int num_terms = corpus_termfrequency.size();
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
            Map<String, Integer> tf_map = map_to_termfrequency_map.get(entry.getKey());
            for (Map.Entry<String, Integer> tf_entry : tf_map.entrySet())
            {
                // change this by using idf maybe?
                document_term_vectors.get(tf_entry.getKey())[index] = Double.valueOf(tf_entry.getValue()) * idf_map.get(tf_entry.getKey());
            }
        }

    }

    public PreQueryCalc(List<ImmutablePair<String, String>> corpus) throws IOException
    {
        this(generate_index(corpus));
        /*reader = generate_index(corpus);
        //extract IDF, tf for evey Document, TF over whole corpus, Document length as tokenized and the collection size from Lucene index.
        collection_size = reader.getDocCount("body");
        total_tokens = reader.getSumTotalTermFreq("body");
        Terms all_terms = MultiTerms.getTerms(reader,"body");
        TermsEnum all_terms_it = all_terms.iterator();
        while (all_terms_it.next() != null){
            BytesRef term = all_terms_it.term();
            corpus_termfrequency.put(term.utf8ToString(), all_terms_it.totalTermFreq());
            Map<String,Integer> tf_map = new HashMap<>();
            idf_map.put(term.utf8ToString(),new ClassicSimilarity().idf(all_terms_it.docFreq(),collection_size));
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id = 0;
            while ((doc_id =postings.nextDoc()) != NO_MORE_DOCS){
                // Add frequencies to Map key for document length! #TODO fix this to readable!
                doc_length_map.put(reader.document(doc_id).getField("title").toString(), doc_length_map.getOrDefault(reader.document(doc_id).getField("title").toString(),0)+ postings.freq());
                tf_map.put(reader.document(doc_id).getField("title").toString(),postings.freq());
            }
            // Put map with Frequency for Terms in all Documents in Termmap, so we can find the Termmap via Term and then find frequency via Document title!
            map_to_termfrequency_map.put(term.utf8ToString(), tf_map);
        }

        for(Map.Entry<String,Integer> entry : doc_length_map.entrySet()){
            total_tokens += entry.getValue();
        }
        for(Map.Entry<String,Long> entry: corpus_termfrequency.entrySet()){
            scs_prob_corpus.put(entry.getKey(), ((double) entry.getValue())/total_tokens);
        }
        // Generate vector space representation
        int num_terms = corpus_termfrequency.size();
        HashMap<String,Integer> term_to_vectorindex = new HashMap<>();
        int i = 0;
        for(Map.Entry<String,Long> entry:corpus_termfrequency.entrySet()){
            term_to_vectorindex.put(entry.getKey(),i);
            i++;
        }
        // fill document vectors with 0s and make Map
        for(int j = 0; j< reader.maxDoc();j++){
            Double[] term_vector = new Double[corpus_termfrequency.size()];
            Arrays.fill(term_vector,0.0);
            document_term_vectors.put(reader.document(j).get("title"),term_vector);
        }
        // get terms and change freq in corresponding vector index for all occurences.
        for(Map.Entry<String,Long> entry:corpus_termfrequency.entrySet()){
            int index = term_to_vectorindex.get(entry.getKey());
            Map<String,Integer> tf_map = map_to_termfrequency_map.get(entry.getKey());
            for(Map.Entry<String,Integer> tf_entry : tf_map.entrySet()){
                // change this by using idf maybe?
                document_term_vectors.get(tf_entry.getKey())[index] = Double.valueOf(tf_entry.getValue())* idf_map.get(tf_entry.getKey());
            }
        }
*/
    }

    /**
     * Generates index for the calculations
     *
     * @param corpus
     * @return
     */
    private static IndexReader generate_index(List<ImmutablePair<String, String>> corpus) throws IOException
    {
        Analyzer analyzer = new EnglishAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        Path indexPath = Files.createTempDirectory("temp");
        FSDirectory dir = FSDirectory.open(indexPath);
        IndexWriter index_writer = new IndexWriter(dir, writerConfig);
        FieldType field = new FieldType();
        field.setTokenized(true);
        field.setStored(true);
        field.setStoreTermVectors(true);
        field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        for (ImmutablePair<String, String> document : corpus)
        {
            String title = document.getLeft();
            String body = document.getRight();
            Document doc = new Document();

            doc.add(new Field("body", title + body, field));
            doc.add(new Field("title", title, StringField.TYPE_STORED));
            index_writer.addDocument(doc);
        }
        index_writer.commit();
        return DirectoryReader.open(index_writer);
    }

    /**
     * @param query
     * @return
     * @throws IOException
     */
    public PrequeryFeatures get_prequery_features(String query) throws IOException
    {
        PrequeryFeatures features = new PrequeryFeatures();
        Analyzer anal = new EnglishAnalyzer();
        TokenStream tokenStream = anal.tokenStream("body", query);
        List<String> tokens = new ArrayList<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken())
        {
            tokens.add(attr.toString());
        }
        tokens = remove_noncorpus_tokens(tokens);
        // #TODO refactor this to filter tokens out first...
        features.idf_features = get_idf_features(tokens);
        features.ictf_features = get_ictf_features(tokens);
        features.entropy_features = get_entropy_features(tokens);
        features.var_features = get_var_features(tokens);
        features.scq_features = get_scq_features(tokens);
        features.query_scope = get_query_scope(tokens);
        features.simplified_clarity_score = get_sclarity_score(tokens);
        features.pmi_features = get_pmi_features(tokens);
        features.coherence_score = get_coherence_score(tokens);
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
            Map<String, Integer> tf_map = map_to_termfrequency_map.get(token);
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
        Map<String, Integer> tf_freqs = map_to_termfrequency_map.get(token);
        if (tf_freqs == null) return null;
        double var_upper_sum = 0;
        double[] wtds = new double[map_to_termfrequency_map.size()];
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
            Map<String, Integer> docs = map_to_termfrequency_map.get(token);
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
            // #TODO Debug remove if no bugs!
            //System.out.println(token+";"+Collections.frequency(tokens,token)+";"+query_size+";"+prob_token_query+";"+prob_token_corpus);
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

    // TODO Refactor this!!!
    private double[] get_pmi_features(List<String> tokens) throws IOException
    {
        double avg_pmi = 0;
        double max_pmi = -Double.MAX_VALUE;
        List<String> token_set = new ArrayList<>();
        IndexSearcher searcher = new IndexSearcher(reader);
        // strip of nonexisten tokens in corpus
        for (String token : tokens)
        {
            if (corpus_termfrequency.get(token) != null) token_set.add(token);
        }
        for (String token_a : token_set)
        {
            for (String token_b : token_set)
            {
                if (token_a.equals(token_b)) continue;
                TermQuery tq_a = new TermQuery(new Term("body", token_a));
                TermQuery tq_b = new TermQuery(new Term("body", token_b));
                BooleanQuery.Builder bqb = new BooleanQuery.Builder();
                bqb.add(tq_a, BooleanClause.Occur.MUST);
                bqb.add(tq_b, BooleanClause.Occur.MUST);
                BooleanQuery bq = bqb.build();
                TopDocs topDocs = searcher.search(bq, Integer.MAX_VALUE);
                double intersect_token_a_token_b = topDocs.scoreDocs.length / ((double) collection_size);
                TopDocs topDocs1 = searcher.search(tq_a, Integer.MAX_VALUE);
                double token_a_prob = topDocs1.scoreDocs.length / ((double) collection_size);
                TopDocs topDocs2 = searcher.search(tq_b, Integer.MAX_VALUE);
                double token_b_prob = topDocs2.scoreDocs.length / ((double) collection_size);
                double tmp_pmi = Math.log(intersect_token_a_token_b / (token_a_prob * token_b_prob));
                avg_pmi += tmp_pmi;
                if (tmp_pmi > max_pmi) max_pmi = tmp_pmi;
            }

        }
        avg_pmi = (2 * (CombinatoricsUtils.factorial(token_set.size() - 1)) / ((double) CombinatoricsUtils.factorial(token_set.size()))) * avg_pmi;
        return new double[]{avg_pmi, max_pmi};
    }

    private double get_coherence_score(List<String> tokens)
    {
        // # TODO factor out this inner looopsss....
        // strip of nonexisten tokens in corpus
        List<String> token_set = new ArrayList<>();
        for (String token : tokens)
        {
            if (corpus_termfrequency.get(token) != null) token_set.add(token);
        }
        // get all  vectors with terms...
        double coherence_score = 0d;
        for (String token : token_set)
        {
            double tmp_score = 0d;
            // get all documents containing term.
            Map<String, Integer> tf_map = map_to_termfrequency_map.get(token);
            // for all documents calculate the sum..
            ArrayList<Map.Entry<String, Integer>> tf_list = new ArrayList<>(tf_map.entrySet());
            for (int i = 0; i < tf_list.size(); i++)
            {
                for (int j = i + 1; j < tf_list.size(); j++)
                {
                    Double[] vector_i = document_term_vectors.get(tf_list.get(i).getKey());
                    Double[] vector_j = document_term_vectors.get(tf_list.get(j).getKey());
                    //# TODO put this into single method
                    double dot_product = 0d;
                    double norm_i = 0d;
                    double norm_j = 0d;
                    for (int k = 0; k < vector_i.length; k++)
                    {
                        dot_product += vector_i[k] + vector_j[k];
                        norm_i += Math.pow(vector_i[k], 2);
                        norm_j += Math.pow(vector_j[k], 2);
                    }
                    norm_i = Math.sqrt(norm_i);
                    norm_j = Math.sqrt(norm_j);
                    tmp_score += dot_product / (norm_i * norm_j);
                }
            }
            coherence_score += tmp_score / (tf_list.size() * tf_list.size() - 1);
        }
        return coherence_score;
    }

}
