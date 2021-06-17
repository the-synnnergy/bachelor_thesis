import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.index.IndexReader;

import java.util.List;

/**
 * Class for Prequery Calculations, in this case all the PreQuery Features defined in DOI:10.1145/3078841.
 * As IDF we will use the Lucene Implemenation here( i think) #TODO
 * Needs to precalculate and save the following:
 * idf for all Terms, tf for All terms, Collection size, Tf for Collection, Collection Size, overall Termfrequency and
 * the number of tokens in the Collection
 */
public class PreQueryCalc {
    private IndexReader reader;
    private List<ImmutablePair<String,String>> corpus;
    private List<ImmutablePair<String,String>> queries;

    public PreQueryCalc(List<ImmutablePair<String, String>> corpus, List<ImmutablePair<String,String>> queries) {
        this.corpus = corpus;
        this.queries= queries;
        generate_index(corpus);

        for (ImmutablePair<String,String> query: queries){

        }


    }

    /**
     * Generates index for the calculations
     * @param corpus
     */
    private void generate_index(List<ImmutablePair<String, String>> corpus) {

    }
}
