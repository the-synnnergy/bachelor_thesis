package IndexStats;

import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.IOException;
import java.util.Map;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

public class IndexStats {

     private Map<Integer,Integer> doc_length_map;
     private Map<Integer,Map<String,Double>> term_probs_per_doc;
     private Map<String,Double> corpus_probs;
     private Map<String,Long> corpus_termfrequency;
     private Map<String,Double> term_idf_map;
     private Map<Integer,Map<String,Integer>> doc_tf_map;
    IndexStats(IndexReader reader) throws IOException {
        Terms all_terms = MultiTerms.getTerms(reader,"body");
        TermsEnum all_terms_it = all_terms.iterator();
        while (all_terms_it.next() != null){
            String term = all_terms_it.term().utf8ToString();
            corpus_termfrequency.put(term,all_terms_it.totalTermFreq());
            term_idf_map.put(term, (double) new ClassicSimilarity().idf(all_terms_it.docFreq(),reader.numDocs()));
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id = 0;
            while ((doc_id =postings.nextDoc()) != NO_MORE_DOCS){
                // Add frequencies to Map key for document length! #TODO fix this to readable!
                doc_length_map.put(doc_id, doc_length_map.getOrDefault(doc_id,0)+ postings.freq());
                //doc_tf_map.put(doc_id,postings.freq());
            }
        }
    }

    public Map<String,Double> get_document_termmap(String doctitle){
        return null;
    };

    public double[] get_document_termvector(String doctile){ return null;}

    public Map<String,Double> get_termfrequency_per_doc(String term){
        return null;
    }

    public double get_termfreq_doc(String term,String doctitle){
        return 0;
    }

    public long get_corpus_termfrequency(String term){
        return 0;
    }

    public int doc_length(int i){
        return 0;
    }

    public double get_corpus_prob(String term){
        return 0;
    }


}
