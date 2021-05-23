import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    IndexSearcher search = null;
    IndexReader reader = null;


    public SimilarityCalc(List<ImmutablePair<String,String>> docs, Similarities simalarity ) throws IOException {
        analyzer = new EnglishAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
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
        FSDirectory dir = FSDirectory.open(indexPath);
        try {
            index_writer = new IndexWriter(dir,writerConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FieldType field = new FieldType();
        field.setTokenized(true);
        field.setStored(true);
        field.setStoreTermVectors(true);
        field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        for (ImmutablePair<String,String> document: docs) {
           String title = document.getLeft();
           String body = document.getRight();
           Document doc = new Document();

           doc.add(new Field("body", title+body, field));
           doc.add(new Field("title", title, StringField.TYPE_STORED));
           index_writer.addDocument(doc);
        }
        index_writer.commit();
        reader = DirectoryReader.open(index_writer);
        search = new IndexSearcher(reader);
        search.setSimilarity(this.sim);
    }

    public List<ImmutablePair<String,Float>> getSimilarities(String query) throws IOException {
        QueryBuilder qb = new QueryBuilder(analyzer);
        Query q = qb.createBooleanQuery("body",query);
        TopDocs top = search.search(q, Integer.MAX_VALUE);
        ScoreDoc[] scoreDocs = top.scoreDocs;
        List<ImmutablePair<String,Float>>  results = new ArrayList<>();
        for(ScoreDoc hit: scoreDocs){
            int docId = hit.doc;
            float score = hit.score;
            String title = search.doc(docId).get("title");
            results.add(new ImmutablePair<String,Float>(title, score));
        }
        return results;
    }
}
