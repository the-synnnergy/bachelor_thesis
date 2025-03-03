package FeaturesCalc;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.io.IOException;
import java.util.*;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

public class Util
{
    /**
     * calculates the cosinus similarity between two document vectors
     * @param a
     * @param b
     * @return
     */
    public static double cos_sim(double[] a, double[] b)
    {
        if (a.length != b.length) throw new DimensionMismatchException(a.length, b.length);
        double dot_product = 0d;
        double norm_i = 0d;
        double norm_j = 0d;
        for (int k = 0; k < a.length; k++)
        {
            dot_product += a[k] + b[k];
            norm_i += Math.pow(a[k], 2);
            norm_j += Math.pow(b[k], 2);
        }
        norm_i = Math.sqrt(norm_i);
        norm_j = Math.sqrt(norm_j);
        return dot_product / (norm_i * norm_j);
    }

    /**
     * Extracts the Terms in a query which are contained in the index too and drops all other terms
     * @param query Query string to be processed
     * @param reader Reader which contains the Terms which should be keeped
     * @param anal Analyzer for tokenization
     * @return List of String containing only the query terms which are in index
     * @throws IOException
     */
    public static List<String> get_query_terms(String query, IndexReader reader, Analyzer anal) throws IOException
    {
        TokenStream tokenStream = anal.tokenStream("body", query);
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        Set<String> terms = new HashSet<>();
        while (all_terms_it.next() != null)
        {
            terms.add(all_terms_it.term().utf8ToString());
        }
        List<String> tokens = new ArrayList<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken())
        {
            tokens.add(attr.toString());
        }
        ArrayList<String> cleaned_tokens = new ArrayList<>();
        for (String token : tokens)
        {
            if (terms.contains(token)) cleaned_tokens.add(token);
        }
        return cleaned_tokens;
    }

    /**
     * returns a map with the term vectors for each document. Mapping from docid to Termvectorarray
     *
     * @param reader Reader from where the documents and vectors are extracted
     * @return Map with document ids to termvectors
     */
    public static Map<Integer, double[]> get_idf_document_vectors(IndexReader reader) throws IOException
    {
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<Integer, double[]> document_vectors = new HashMap<>();
        int length = 0;
        // get length of term vectors
        while (all_terms_it.next() != null)
        {

            length++;
        }
        int num_docs = reader.numDocs();
        // create empty termvectors
        for (int i = 0; i < num_docs; i++)
        {
            double[] term_vector = new double[length];
            document_vectors.put(i, term_vector);
        }
        all_terms_it = all_terms.iterator();
        int pos = 0;
        // fill termvectors
        while (all_terms_it.next() != null)
        {
            long total_freq = all_terms_it.totalTermFreq();
            long doc_freq = all_terms_it.docFreq();
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id;
            double idf = (1 + Math.log(((double) doc_freq + 1) / (total_freq + 1)));
            while ((doc_id = postings.nextDoc()) != NO_MORE_DOCS)
            {
                double[] term_vector = document_vectors.get(doc_id);
                term_vector[pos] = postings.freq() * idf;
                document_vectors.put(doc_id, term_vector);
            }
            //System.out.println(pos + ":" + all_terms_it.term().utf8ToString());
            pos++;
        }
        return document_vectors;
    }

    /**
     * Mapping from Termvector index to Term as String
     * @param reader IndexReader from where the terms are extracted
     * @return Map which connects termvector index with the term String value
     * @throws IOException
     */
    public static Map<Integer, String> get_termvector_terms(IndexReader reader) throws IOException
    {
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<Integer, String> termvector_terms = new HashMap<>();
        int i = 0;
        while (all_terms_it.next() != null)
        {
            termvector_terms.put(i, all_terms_it.term().utf8ToString());
            i++;
        }
        return termvector_terms;
    }

    /**
     * Gets the idf termvector from the reader
     * @param reader reader from where
     * @return idf termvector
     * @throws IOException
     */
    public static double[] get_idf_termvectors(IndexReader reader) throws IOException
    {
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        // well better assume our terms arent going to explode! :)
        //System.out.println("Count of Terms:"+all_terms.size());
        int length = 0;
        while (all_terms_it.next() != null)
        {
            length++;
        }
        all_terms_it = all_terms.iterator();
        //System.out.println("lenght"+length);
        ClassicSimilarity sim = new ClassicSimilarity();
        double[] idf_termvector = new double[length];
        int i = 0;
        while (all_terms_it.next() != null)
        {
            //System.out.println("idf:"+sim.idf(all_terms_it.docFreq(), reader.numDocs()));
            idf_termvector[i] = sim.idf(all_terms_it.docFreq(), reader.numDocs());
            i++;
        }
        return idf_termvector;
    }

    /**
     * get the idf termvector for the query
     * @param query query for which the termvector is created
     * @param reader reader which contains the terms and their idfs
     * @return idf termvector for query string
     * @throws IOException
     */
    public static double[] get_query_idf_termvector(String query, IndexReader reader) throws IOException
    {
        Analyzer anal = new EnglishAnalyzer();
        TokenStream tokenStream = anal.tokenStream("body", query);
        Map<String, Integer> token_counts = new HashMap<>();
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken())
        {
            token_counts.merge(attr.toString(), 1, Integer::sum);
        }
        Map<Integer, String> termvector_ids = get_termvector_terms(reader);
        double[] termvector = get_idf_termvectors(reader);
        for (int i = 0; i < termvector.length; i++)
        {
            termvector[i] = token_counts.getOrDefault(termvector_ids.get(i), 0) * termvector[i];
        }
        return termvector;
    }

    /**
     *  Calculates the probalities
     * @param reader
     * @return
     * @throws IOException
     */
    public static Map<Integer, Map<String, Double>> get_document_probs(IndexReader reader) throws IOException
    {
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<Integer, double[]> document_vectors = new HashMap<>();
        Map<Integer, String> terms_map = get_termvector_terms(reader);
        Map<Integer, Map<String, Double>> doc_probs = new HashMap<>();
        int num_docs = reader.numDocs();
        int length = 0;
        while (all_terms_it.next() != null)
        {

            length++;
        }

        for (int i = 0; i < num_docs; i++)
        {
            double[] term_vector = new double[length];
            document_vectors.put(i, term_vector);
        }
        all_terms_it = all_terms.iterator();
        int pos = 0;
        while (all_terms_it.next() != null)
        {
            PostingsEnum postings = all_terms_it.postings(null);
            int doc_id = 0;
            while ((doc_id = postings.nextDoc()) != NO_MORE_DOCS)
            {
                double[] term_vector = document_vectors.get(doc_id);
                term_vector[pos] = postings.freq();
                document_vectors.put(doc_id, term_vector);
            }
            //System.out.println(pos+":"+all_terms_it.term().utf8ToString());
            pos++;
        }
        for (int i = 0; i < num_docs; i++)
        {
            double[] term_vector = calc_prob_vector(document_vectors.get(i));
            //System.out.println(Arrays.toString(term_vector));
            Map<String, Double> probs = new HashMap<>();
            for (int j = 0; j < term_vector.length; j++)
            {
                probs.put(terms_map.get(j), term_vector[j]);
            }
            doc_probs.put(i, probs);
        }
        return doc_probs;
    }

    private static double[] calc_prob_vector(double[] termvector)
    {
        double freq_sum = Arrays.stream(termvector).parallel().sum();
        for (int i = 0; i < termvector.length; i++)
        {
            termvector[i] = termvector[i] / freq_sum;
        }
        return termvector;
    }

    public static Map<String, Double> get_corpus_probs(IndexReader reader) throws IOException
    {
        Terms all_terms = MultiTerms.getTerms(reader, "body");
        TermsEnum all_terms_it = all_terms.iterator();
        Map<String, Double> corpus_probs = new HashMap<>();
        Map<Integer, String> terms_map = get_termvector_terms(reader);
        long total_corpus_term_count = reader.getSumTotalTermFreq("body");
        while (all_terms_it.next() != null)
        {
            corpus_probs.put(all_terms_it.term().utf8ToString(), ((double) all_terms_it.totalTermFreq()) / total_corpus_term_count);
        }

        return corpus_probs;
    }

}
