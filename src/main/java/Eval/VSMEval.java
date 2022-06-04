package Eval;

import FeaturesCalc.FeaturesCalc;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static IndexTools.IndexCreator.createIndices;
import static Parser.iTrustParser.get_NameFileMap;

public class VSMEval
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

        int[] numberOfDocsToRetrieve = new int[]{30,40,50,60,70,100,150,200};
        double[] percentageToRetrieve = new double[]{0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1};

        for(int i : numberOfDocsToRetrieve)
        {
            evaluateIRDocWise(i,req_readers, java_readers);
        }
        for(double i : percentageToRetrieve)
        {
            evaluateIRPercentageWise(i,req_readers,java_readers);
        }
        // evaluate different scores!
        // evaluate for 30 , 40 , 50 , 60 , 70 , 100 , 150 , 200 Documents
        // evaluate for 90, 80, 70 , 60 , 40 , 50 , 30 , 20 , 10 %
    }

    private static void evaluateIRPercentageWise(double i, IndexReader[] reader_req, IndexReader[] java_readers)
    {

    }

    private static void evaluateIRDocWise(int num, IndexReader[] reader_req, IndexReader[] java_readers)
    {

    }
}
