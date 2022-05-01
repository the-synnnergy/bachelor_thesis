import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static IndexTools.IndexCreator.createIndices;
import static XMLParser.iTrustParser.get_NameFileMap;
import static XMLParser.iTrustParser.get_true_req_to_source_links;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {
        Map<String,String> name_to_file = get_NameFileMap("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/source_req.xml");
        String[] index_paths = createIndices("index_test/",name_to_file);
        Directory dir = FSDirectory.open(Paths.get("./index_test/BM_25"));
        IndexReader reader = DirectoryReader.open(dir);
        for (int i = 0; i< reader.numDocs();i++)
        {
            System.out.println(reader.document(i).getField("body").stringValue());
        }


        //Map<String, List<String>> b = get_true_req_to_source_links("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/answer_req_code.xml");
    }
}
