package IndexTools;

import FeaturesCalc.FeaturesCalc;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class IndexCreator
{
    public static String[] createIndices(String index_basepath, Map<String, String> path_to_content) throws IOException
    {
        String[] index_paths = new String[FeaturesCalc.Similarities.values().length];
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            index_paths[sim.ordinal()] = createIndex(sim, index_basepath, path_to_content);
        }
        return index_paths;
    }

    private static String createIndex(FeaturesCalc.Similarities sim, String index_basepath, Map<String, String> name_to_content) throws IOException
    {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new EnglishAnalyzer());
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        String index_path = index_basepath + sim.name();
        Directory dir = FSDirectory.open(Paths.get(index_path));
        switch (sim)
        {
            case JELINEK_MERCER:
                indexWriterConfig.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
                break;

            case BM_25:
                indexWriterConfig.setSimilarity(new BM25Similarity());
                break;
            case CLASSIC_SIM:
                indexWriterConfig.setSimilarity(new ClassicSimilarity());
                break;
            case DIRIRCHLET:
                indexWriterConfig.setSimilarity(new LMDirichletSimilarity());
                break;
        }
        IndexWriter indexWriter = new IndexWriter(dir, indexWriterConfig);

        for (Map.Entry<String, String> entry : name_to_content.entrySet())
        {
            try
            {
                Document doc = new Document();
                FieldType body_field_type = new FieldType();
                body_field_type.setTokenized(true);
                body_field_type.setStored(true);
                body_field_type.setStoreTermVectors(true);
                body_field_type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
                Path file = Path.of(entry.getValue());
                String body = Files.readString(file, StandardCharsets.ISO_8859_1);
                Field body_field = new Field("body", body, body_field_type);
                doc.add(body_field);
                doc.add(new StringField("title", entry.getKey(), Field.Store.YES));
                indexWriter.addDocument(doc);
            } catch (java.nio.charset.MalformedInputException e)
            {
                System.out.println("File not found:"+entry.getValue());
            }
        }

        indexWriter.commit();
        indexWriter.close();

        return index_path;
    }
}
