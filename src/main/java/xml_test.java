import FeaturesCalc.FeaturesCalc;
import FeaturesCalc.InstanceData;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Attr;
import org.xml.sax.SAXException;
import weka.core.Attribute;
import weka.core.DenseInstance;
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


        Map<String, String> name_to_file = get_NameFileMap("/home/marcel/Downloads/SMOS/", "/home/marcel/Downloads/SMOS/source_req.xml");
        String[] index_paths_req = createIndices("index_test/reqs/", name_to_file);
        Directory dir = FSDirectory.open(Paths.get(index_paths_req[0]));
        Map<String, String> name_to_java = get_NameFileMap("/home/marcel/Downloads/SMOS/", "/home/marcel/Downloads/SMOS/target_code.xml");
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
        Instances dataset = new Instances("SMOS", (ArrayList<Attribute>) att, 50000);
        dataset.setClassIndex(dataset.numAttributes() - 1);
        Map<String, List<String>> b = get_true_req_to_source_links("/home/marcel/Downloads/SMOS/", "/home/marcel/Downloads/SMOS/answer_req_code.xml");
        for (InstanceData instanceData : data)
        {
            //rewrite this return the double array, add
            List<Double> instanceAttributes = instanceData.getUnlabeledWekaInstance();


            List<String> targets = b.get(instanceData.getIdentifierQuery());
            if (targets == null || !targets.contains(instanceData.getIdentifierTarget()))
            {
                instanceAttributes.add(0.0d);
                Instance instance = new DenseInstance(1, instanceAttributes.stream().mapToDouble(d -> d).toArray());
                instance.setDataset(dataset);
                dataset.add(instance);
                continue;
            }
            instanceAttributes.add(1.0d);
            Instance instance = new DenseInstance(1, instanceAttributes.stream().mapToDouble(d -> d).toArray());
            instance.setDataset(dataset);
            dataset.add(instance);

        }
        try
        {
            ConverterUtils.DataSink.write("dataset.arff", dataset);

        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }


    }}
