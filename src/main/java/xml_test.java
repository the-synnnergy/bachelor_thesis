import FeaturesCalc.PostQueryCalc;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import static IndexTools.IndexCreator.createIndices;
import static Parser.iTrustParser.get_NameFileMap;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {
        Map<String,String> name_to_file = get_NameFileMap("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/source_req.xml");
        String[] index_paths_req = createIndices("index_test/reqs/",name_to_file);
        Directory dir = FSDirectory.open(Paths.get(index_paths_req[0]));
        Map<String,String> name_to_java = get_NameFileMap("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/target_code.xml");
        String[] index_paths_java = createIndices("index_test/java/",name_to_java);
        IndexReader reader_req = DirectoryReader.open(dir);
        /*for (int i = 0; i< reader.numDocs();i++)
        {
            System.out.println(reader.document(i).getField("body").stringValue());
        }*/
        PostQueryCalc pq = new PostQueryCalc(reader_req, new EnglishAnalyzer());
        IndexReader reader_java = DirectoryReader.open(FSDirectory.open(Paths.get(index_paths_java[0])));
        String query = reader_java.document(0).getField("body").stringValue();
        System.out.println(query);
        PostQueryCalc.PostQueryFeatures pq_feat = pq.get_PostQueryFeatures(query);
        pq_feat.print();


        //Map<String, List<String>> b = get_true_req_to_source_links("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/answer_req_code.xml");
    }
}
