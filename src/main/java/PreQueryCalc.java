import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.comparators.DoubleComparator;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.math3.stat.StatUtils.populationVariance;
import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

/**
 * Class for Prequery Calculations, in this case all the PreQuery Features defined in DOI:10.1145/3078841.
 * As IDF we will use the Lucene Implemenation here( i think) #TODO
 * Needs to precalculate and save the following:
 * idf for all Terms, tf for All terms, Collection size, Tf for Collection, Collection Size, overall Termfrequency and
 * the number of tokens in the Collection
 */
public class PreQueryCalc {

    public class PrequeryFeatures {
        double[] idf_features;
        double[] ictf_features;
        double[] entropy_features;
        double[] var_features;
        double[] scq_features;
        double[] pmi_features;
        double query_scope;
        double simplified_clarity_score;
        double coherence_score;
    }
    private List<ImmutablePair<String,String>> corpus;
    private int collection_size;
    private Map<String,Long> corpus_termfrequency;
    private Map<String,Map<String,Integer>> map_to_termfrequency_map;
    private Map<String, Float> idf_map;
    private long total_tokens;
    public PreQueryCalc(List<ImmutablePair<String, String>> corpus) throws IOException {
        this.corpus = corpus;
        IndexReader reader = generate_index(corpus);
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
            int doc_id = postings.nextDoc();
            while (postings.nextDoc() != NO_MORE_DOCS){
                tf_map.put(reader.document(doc_id).getField("title").toString(),postings.freq());
            }
            map_to_termfrequency_map.put(term.utf8ToString(), tf_map);
        }
        // #TODO length of the documents!(Should i use the tokenized length or normal length? Mostly tokenized, because i use the tokenized freqs everywhere
        /**
         * List<Terms> term_vectors = new ArrayList<>();
         *         for(int i = 0;i<collection_size;i++){
         *             term_vectors.add(reader.getTermVector(i,"body"));
         *         }
         */



    }

    /**
     * Generates index for the calculations
     * @param corpus
     * @return
     */
    private IndexReader generate_index(List<ImmutablePair<String, String>> corpus) throws IOException {
        Analyzer analyzer = new EnglishAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        Path indexPath = Files.createTempDirectory("temp" );
        FSDirectory dir = FSDirectory.open(indexPath);
        IndexWriter index_writer = new IndexWriter(dir,writerConfig);
        FieldType field = new FieldType();
        field.setTokenized(true);
        field.setStored(true);
        field.setStoreTermVectors(true);
        field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        for (ImmutablePair<String,String> document: corpus) {
            String title = document.getLeft();
            String body = document.getRight();
            Document doc = new Document();

            doc.add(new Field("body", title+body, field));
            doc.add(new Field("title", title, StringField.TYPE_STORED));
            index_writer.addDocument(doc);
        }
        index_writer.commit();
        IndexReader reader = DirectoryReader.open(index_writer);
        return reader;
    }

    /**
     *
     * @param query
     * @return
     * @throws IOException
     */
    public PrequeryFeatures get_prequery_features(String query) throws IOException {
        PrequeryFeatures features = new PrequeryFeatures();
        Analyzer anal = new EnglishAnalyzer();
        TokenStream tokenStream =anal.tokenStream("body",query);
        List<String> tokens = new ArrayList<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()){
            tokens.add(attr.toString());
        }
        features.idf_features = get_idf_features(tokens);
        return features;
    }

    /**
     * Calclates the idf features for the given tokens!
     * @param tokens tokenized query (same analyzer type(with same stop words!) needed as on the index!)
     * @return  Array with idf_features, avg, max and dev! #TODO USE CONSTANTS FOR ACCESSING ARRAY POSITIONS!
     */
    private double[] get_idf_features(List<String> tokens) {
        double avg_idf = 0;
        double max_idf = 0;
        double dev_idf = 0;
        int query_terms = 0;
        Float tmp = null;
        List<Double> terms_idf = new ArrayList<>();
        for (String token: tokens){
            tmp = idf_map.get(token);
            if(tmp != null){
                terms_idf.add(tmp.doubleValue());
                query_terms++;
                if(tmp > max_idf) max_idf = tmp;
                avg_idf += tmp;
            }
        }
        avg_idf = avg_idf * (1.0d/query_terms);
        dev_idf = populationVariance(terms_idf.stream().mapToDouble(d -> d).toArray(),avg_idf);
        return new double[]{avg_idf,max_idf,dev_idf};
    }

    /**
     *
     * @param tokens
     * @return
     */
    private double[] get_ictf_features(List<String> tokens){
        double avg_ictf = 0;
        double max_ictf = 0;
        double dev_ictf = 0;
        int query_terms = 0;
        List<Double> terms_ictf = new ArrayList<>();
        Long tmp_tf = null;
        for(String token: tokens){
            tmp_tf = corpus_termfrequency.get(token);
            if(tmp_tf != null){
                double tmp_ictf = Math.log(((double)collection_size)/tmp_tf)/Math.log(2);
                if(tmp_ictf > max_ictf) max_ictf = tmp_ictf;
                avg_ictf += tmp_ictf;
                query_terms++;
                terms_ictf.add(tmp_ictf);
            }
        }
        avg_ictf = avg_ictf *(1.0d/query_terms);
        dev_ictf = populationVariance(terms_ictf.stream().mapToDouble(d -> d).toArray(),avg_ictf);
        return new double[]{avg_ictf,max_ictf,dev_ictf};
    }

    /**
     *
     * @param tokens
     * @return
     */
    private double[] get_entropy_features(List<String> tokens){
        double avg_entropy = 0;
        double median_entropy = 0;
        double max_entropy = 0;
        int query_terms = 0;
        Integer term_frequency = 0;
        List<Double> entropies = new ArrayList<>();
        for(String token: tokens){
            Map<String,Integer> tf_map= map_to_termfrequency_map.get(token);
            if(tf_map == null) continue;
            query_terms++;
            double tmp_entropy = 0;
            long corpus_tf = corpus_termfrequency.get(token);
            for (Map.Entry<String,Integer> entry : tf_map.entrySet()){
                tmp_entropy +=(((double)entry.getValue())/corpus_tf)*(Math.log(((double)entry.getValue())/corpus_tf)/Math.log(collection_size));
            }
            if(tmp_entropy > max_entropy) max_entropy = tmp_entropy;
            entropies.add(tmp_entropy);
            avg_entropy += avg_entropy;
        }
        avg_entropy = avg_entropy/collection_size;
        median_entropy = new Median().evaluate(entropies.stream().mapToDouble(d -> d).toArray());
        return new double[]{avg_entropy,median_entropy,max_entropy};
    }

}
