import FeaturesCalc.FeaturesCalc;
import FeaturesCalc.PostQueryCalc;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import static IndexTools.IndexCreator.createIndices;
import static Parser.iTrustParser.get_NameFileMap;
import static Parser.iTrustParser.get_true_req_to_source_links;
import FeaturesCalc.InstanceData;
import weka.core.Attribute;

import static FeaturesCalc.FeaturesCalc.get_full_dataset;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {
        /*Map<String,String> name_to_file = get_NameFileMap("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/source_req.xml");
        String[] index_paths_req = createIndices("index_test/reqs/",name_to_file);
        Directory dir = FSDirectory.open(Paths.get(index_paths_req[0]));
        Map<String,String> name_to_java = get_NameFileMap("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/target_code.xml");
        String[] index_paths_java = createIndices("index_test/java/",name_to_java);
        IndexReader reader_req = DirectoryReader.open(dir);
        *//*for (int i = 0; i< reader.numDocs();i++)
        {
            System.out.println(reader.document(i).getField("body").stringValue());
        }*//*
        *//*System.out.println(query);
        PostQueryCalc.PostQueryFeatures pq_feat = pq.get_PostQueryFeatures(query);
        pq_feat.print();*//*
        IndexReader[] req_readers = new IndexReader[FeaturesCalc.Similarities.values().length];
        IndexReader[] java_readers = new IndexReader[FeaturesCalc.Similarities.values().length];
        for(FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            req_readers[sim.ordinal()] = DirectoryReader.open(FSDirectory.open(Paths.get(index_paths_req[sim.ordinal()])));
            java_readers[sim.ordinal()] = DirectoryReader.open(FSDirectory.open(Paths.get(index_paths_java[sim.ordinal()])));
        }
        System.out.println("begin calculating features");
        List<InstanceData> data = get_full_dataset(req_readers,java_readers,null);
        System.out.println("Test");

        Map<String, List<String>> b = get_true_req_to_source_links("/home/marcel/Downloads/iTrust/","/home/marcel/Downloads/iTrust/answer_req_code.xml");
        for(InstanceData instanceData : data)
        {

        }*/
        List<Attribute> att = InstanceData.attributesAsList();
        for(Attribute attribute : att)
        {
            System.out.println(attribute.name());
        }
    }

    // #TODO move this to right package!
    private static void create_csv_dataset(List<InstanceData> data, Map<String,List<String>> links, String path)
    {

        for(InstanceData instanceData : data)
        {
            //String arr[] = instanceData.toStringArr();
        }
    }
}
