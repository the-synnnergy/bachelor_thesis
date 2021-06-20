import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
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
        }
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
     * @return  Array with idf_features, avg, max and dev! #TODO USE CONSTANTS FOR ACCESING ARRAY POSITIONS!
     */
    private double[] get_idf_features(List<String> tokens) {
        double avg_idf = 0;
        double max_idf = 0;
        double dev_idf = 0;
        int docs = 0;
        Float tmp = null;
        List<Double> terms_idf = new ArrayList<>();
        for (String token: tokens){
            tmp = idf_map.get(token);
            if(tmp != null){
                terms_idf.add(tmp.doubleValue());
                docs++;
                if(tmp > max_idf) max_idf = tmp;
                avg_idf += tmp;
            }
        }
        avg_idf = avg_idf * (1.0d/tokens.size());
        dev_idf = populationVariance(terms_idf.stream().mapToDouble(d -> d).toArray(),avg_idf);
        return new double[]{avg_idf,max_idf,dev_idf};
    }

}
