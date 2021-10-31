package IndexStats;

import org.apache.lucene.index.IndexReader;

import java.util.Map;

public class IndexStats {

     private Map<Integer,Integer> doc_length_map;
     private Map<Integer,Map<String,Double>> term_probs_per_doc;
     private Map<String,Double> corpus_probs;

    IndexStats(IndexReader reader){

    }

    public Map<String,Double> get_document_termvector(String doctitle){
        return null;
    };

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
