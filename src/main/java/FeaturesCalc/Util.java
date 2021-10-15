package FeaturesCalc;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.*;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

public class Util {
    public static List<Term> extract_terms_boolean_query(BooleanQuery query){
        List<Term> terms = new ArrayList<>();
        for(BooleanClause clause: query.clauses()){
            terms.add(((TermQuery)clause.getQuery()).getTerm());
        }
        return terms;
    }

    public static double cos_sim(double[] a, double[] b){
        if(a.length != b.length) throw new DimensionMismatchException(a.length,b.length);
        double dot_product = 0d;
        double norm_i = 0d;
        double norm_j = 0d;
        for(int k = 0; k< a.length;k++){
            dot_product += a[k] +b[k];
            norm_i += Math.pow(a[k],2);
            norm_j += Math.pow(b[k],2);
        }
        norm_i = Math.sqrt(norm_i);
        norm_j = Math.sqrt(norm_j);
        return dot_product/(norm_i*norm_j);
    }

    public static List<String> get_query_terms(String query, Collection<String> corpus_terms, Analyzer anal) throws IOException {
        TokenStream tokenStream = anal.tokenStream("body", query);
        List<String> tokens = new ArrayList<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokens.add(attr.toString());
        }
        ArrayList<String> cleaned_tokens = new ArrayList<>();
        for (String token : tokens) {
            if (corpus_terms.contains(token)) cleaned_tokens.add(token);
        }
        return cleaned_tokens;
    }

    /**
     * returns a map with the term vectors for each document. Mapping from docid to Termvectorarray
     * @param reader
     * @return
     */
    // #TODO do IDF at end!!!!
    public static Map<Integer, int[]> get_idf_document_vectors(IndexReader reader) throws IOException {
        Terms all_terms = MultiTerms.getTerms(reader,"body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<Integer,int[]> document_vectors = new HashMap<>();
        int length = 0;
        while(all_terms_it.next() != null){
            length++;
        }
        int num_docs = reader.numDocs();
        // try this if not working change #TODO
        for(int i = 0; i < num_docs;i++){
            int[] term_vector = new int[length];
            document_vectors.put(i,term_vector);
        }
        all_terms_it = all_terms.iterator();
        int pos = 0;
        while (all_terms_it.next() != null){
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id = 0;
            while ((doc_id = postings.nextDoc()) != NO_MORE_DOCS){
                int[] term_vector = document_vectors.get(doc_id);
                term_vector[pos] = postings.freq();
                document_vectors.put(doc_id,term_vector);
            }
            System.out.println(pos+":"+all_terms_it.term().utf8ToString());
            pos++;
        }
        return document_vectors;
    };

    public static Map<Integer,String> get_termvector_terms(IndexReader reader) throws IOException{
        Terms all_terms = MultiTerms.getTerms(reader,"body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<Integer,String> termvector_terms = new HashMap<>();
        int i = 0;
        while (all_terms_it.next() != null){
            termvector_terms.put(i,all_terms_it.term().utf8ToString());
            i++;
        }
        return termvector_terms;
    };

    // #TODO get idf termvectors!!!
    public static int[] get_query_idf_termvector(String query, IndexReader reader) throws IOException {
        Analyzer anal = new EnglishAnalyzer();
        TokenStream tokenStream = anal.tokenStream("body", query);
        Map<String, Integer>  token_counts = new HashMap<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
           token_counts.merge(attr.toString(),1,Integer::sum);
        }
        Map<Integer,String> termvector_ids = get_termvector_terms(reader);
        int[] termvector = new int[termvector_ids.size()];
        for(int i = 0;i < termvector.length;i++){
            termvector[i] = token_counts.getOrDefault(termvector_ids.get(i),0);
        }
        return  termvector;
    };
}
