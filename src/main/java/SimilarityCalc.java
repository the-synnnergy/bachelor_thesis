import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

enum Similarities{
    BM25,
    TF_IDF,
    JMSim,
    Dirichlet
};
public class SimilarityCalc {
    /*
    Variables for the reader, writer index stuff and Similarity.
     */
    IndexWriter index_writer = null;
    Similarity sim = null;
    Analyzer analyzer = null;

    private SimilarityCalc() {
    }

    public SimilarityCalc(List<ImmutablePair<String,String>> docs, Similarities simalarity ) {
        analyzer = new EnglishAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        switch (simalarity) {
            case BM25:
                sim = new BM25Similarity();
                break;
            case TF_IDF:
                sim = new ClassicSimilarity();
                break;
            case JMSim:
                sim = new LMJelinekMercerSimilarity(0.7f);
                break;
            case Dirichlet:
                sim = new LMDirichletSimilarity();
                break;
        }
        writerConfig.setSimilarity(sim);
        Path indexPath = null;
        try {
            indexPath = Files.createTempDirectory("temp" );
        } catch (IOException e) {
            e.printStackTrace();

        }
        assert indexPath != null;
        // not sure if good.... maybe new fsdirectory better, dont know if its working
        try {
            index_writer = new IndexWriter((Directory) indexPath,writerConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
