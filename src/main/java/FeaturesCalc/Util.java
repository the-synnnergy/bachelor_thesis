package FeaturesCalc;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    public static Map<String, Double> get_document_vectors(IndexReader reader) {
        return null;
    }
}
