import FeaturesCalc.FeaturesCalc;
import FeaturesCalc.InstanceData;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Attr;
import org.xml.sax.SAXException;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static FeaturesCalc.FeaturesCalc.get_full_dataset;
import static IndexTools.IndexCreator.createIndices;
import static Parser.iTrustParser.get_NameFileMap;
import static Parser.iTrustParser.get_true_req_to_source_links;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {



        Map<String, String> name_to_file = get_NameFileMap("/home/marcel/Downloads/iTrust/", "/home/marcel/Downloads/iTrust/source_req.xml");
        String[] index_paths_req = createIndices("index_test/reqs/", name_to_file);
        Directory dir = FSDirectory.open(Paths.get(index_paths_req[0]));
        Map<String, String> name_to_java = get_NameFileMap("/home/marcel/Downloads/iTrust/", "/home/marcel/Downloads/iTrust/target_code.xml");
        String[] index_paths_java = createIndices("index_test/java/", name_to_java);
        IndexReader reader_req = DirectoryReader.open(dir);

        IndexReader[] req_readers = new IndexReader[FeaturesCalc.Similarities.values().length];
        IndexReader[] java_readers = new IndexReader[FeaturesCalc.Similarities.values().length];
        for (FeaturesCalc.Similarities sim : FeaturesCalc.Similarities.values())
        {
            req_readers[sim.ordinal()] = DirectoryReader.open(FSDirectory.open(Paths.get(index_paths_req[sim.ordinal()])));
            java_readers[sim.ordinal()] = DirectoryReader.open(FSDirectory.open(Paths.get(index_paths_java[sim.ordinal()])));
        }
        System.out.println("begin calculating features");
        List<InstanceData> data = get_full_dataset(req_readers, java_readers, null);
        System.out.println("Test");
        // TODO check if this is the same length as getUnlabeledWekaInstance
        List<Attribute> att = InstanceData.attributesAsList();
        att.add(new Attribute("class"));
        Instances dataset = new Instances("iTrust", (ArrayList<Attribute>) att, 20000);
        dataset.setClassIndex(dataset.numAttributes() -1);
        Map<String, List<String>> b = get_true_req_to_source_links("/home/marcel/Downloads/iTrust/", "/home/marcel/Downloads/iTrust/answer_req_code.xml");
        for (InstanceData instanceData : data)
        {
            //rewrite this return the double array, add
            Instance instance = instanceData.getUnlabeledWekaInstance();
            instance.setDataset(dataset);
            instance.setClassValue(0);
            dataset.add(instanceData.getUnlabeledWekaInstance());
        }
        try
        {
            ConverterUtils.DataSink.write("dataset.arff", dataset);

        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        /*for(Attribute attribute : att)
        {
            System.out.println(attribute.name());
        }*/
    }

    // #TODO move this to right package!
    private static void create_csv_dataset(List<InstanceData> data, Map<String, List<String>> links, String path)
    {

        for (InstanceData instanceData : data)
        {
            //String arr[] = instanceData.toStringArr();
        }
    }
}
