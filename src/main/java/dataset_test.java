import FeaturesCalc.InstanceData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static FeaturesCalc.FeaturesCalc.get_full_dataset;

public class dataset_test
{
    public static void main(String[] args) throws IOException
    {
        Analyzer anal = new EnglishAnalyzer();
        File index_vsm = new File("index_vsm");
        File index_jm = new File("index_jm");
        File index_dirichlet = new File("index_dirichlet");
        File index_bm25 = new File("index_bm25");
        String a = Files.readString(Path.of("51121"));
        String b = Files.readString(Path.of("51126"));
        Document doc = new Document();
        FieldType field = new FieldType();
        field.setTokenized(true);
        field.setStored(true);
        field.setStoreTermVectors(true);
        field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field fd = new Field("body", a, field);
        doc.add(fd);
        doc.add(new StringField("title", "51121", Field.Store.YES));
        Directory dir_bm25 = FSDirectory.open(index_bm25.toPath());
        Directory dir_vsm = FSDirectory.open(index_vsm.toPath());
        Directory dir_jm = FSDirectory.open(index_jm.toPath());
        Directory dir_dirichlet = FSDirectory.open(index_dirichlet.toPath());
        IndexWriterConfig writerConfig_bm25 = new IndexWriterConfig(anal);
        writerConfig_bm25.setSimilarity(new BM25Similarity());
        IndexWriter writer_bm25 = new IndexWriter(dir_bm25,writerConfig_bm25);
        IndexWriterConfig writerConfig_vsm = new IndexWriterConfig(anal);
        writerConfig_vsm.setSimilarity(new ClassicSimilarity());
        IndexWriter writer_vsm = new IndexWriter(dir_vsm,writerConfig_vsm);
        IndexWriterConfig writerConfig_jm = new IndexWriterConfig(anal);
        writerConfig_jm.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
        IndexWriter writer_jm = new IndexWriter(dir_jm,writerConfig_jm);
        IndexWriterConfig writerConfig_dirichlet = new IndexWriterConfig(anal);
        writerConfig_dirichlet.setSimilarity(new LMDirichletSimilarity());
        IndexWriter writer_dirichlet = new IndexWriter(dir_dirichlet,writerConfig_dirichlet);
        writer_dirichlet.addDocument(doc);
        writer_bm25.addDocument(doc);
        writer_vsm.addDocument(doc);
        writer_jm.addDocument(doc);


        File index_vsm_target = new File("index_vsm_target");
        File index_jm_target = new File("index_jm_target");
        File index_dirichlet_target = new File("index_dirichlet_target");
        File index_bm25_target = new File("index_bm25_target");
        Document doc_target = new Document();
        FieldType field_target = new FieldType();
        field_target.setTokenized(true);
        field_target.setStored(true);
        field_target.setStoreTermVectors(true);
        field_target.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field fd_target = new Field("body", a, field_target);
        doc_target.add(fd_target);
        doc_target.add(new StringField("title", "51126", Field.Store.YES));
        Directory dir_bm25_target = FSDirectory.open(index_bm25_target.toPath());
        Directory dir_vsm_target = FSDirectory.open(index_vsm_target.toPath());
        Directory dir_jm_target = FSDirectory.open(index_jm_target.toPath());
        Directory dir_dirichlet_target = FSDirectory.open(index_dirichlet_target.toPath());
        IndexWriterConfig writerConfig_bm25_target = new IndexWriterConfig(anal);
        writerConfig_bm25_target.setSimilarity(new BM25Similarity());
        IndexWriter writer_bm25_target = new IndexWriter(dir_bm25_target,writerConfig_bm25_target);
        IndexWriterConfig writerConfig_vsm_target = new IndexWriterConfig(anal);
        writerConfig_vsm_target.setSimilarity(new ClassicSimilarity());
        IndexWriter writer_vsm_target = new IndexWriter(dir_vsm_target,writerConfig_vsm_target);
        IndexWriterConfig writerConfig_jm_target = new IndexWriterConfig(anal);
        writerConfig_jm.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
        IndexWriter writer_jm_target = new IndexWriter(dir_jm_target,writerConfig_jm_target);
        IndexWriterConfig writerConfig_dirichlet_target = new IndexWriterConfig(anal);
        writerConfig_dirichlet_target.setSimilarity(new LMDirichletSimilarity());
        IndexWriter writer_dirichlet_target = new IndexWriter(dir_dirichlet_target,writerConfig_dirichlet_target);
        writer_dirichlet_target.addDocument(doc);
        writer_bm25_target.addDocument(doc);
        writer_vsm_target.addDocument(doc);
        writer_jm_target.addDocument(doc);
        IndexReader[] query_reader = new IndexReader[]{DirectoryReader.open(writer_bm25),DirectoryReader.open(writer_vsm),DirectoryReader.open(writer_jm),DirectoryReader.open(writer_dirichlet)};
        IndexReader[] target_reader = new IndexReader[]{DirectoryReader.open(writer_bm25_target),DirectoryReader.open(writer_vsm_target),DirectoryReader.open(writer_jm_target),DirectoryReader.open(writer_dirichlet_target)};
        List<InstanceData> instanceData =  get_full_dataset(query_reader,target_reader,null);
        FileWriter writer = new FileWriter("output.txt");
        for(InstanceData instance : instanceData){
            List<Pair<String, Double>> data = instance.get_iterableList();
            for(Pair<String,Double> value : data){
                writer.write(value.getLeft()+":"+value.getRight());
            }
        }

    }
}
