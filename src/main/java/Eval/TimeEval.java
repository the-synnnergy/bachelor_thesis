package Eval;

import FeaturesCalc.FeaturesCalc;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.similarities.BM25Similarity;
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
import FeaturesCalc.PostRetrievalCalc;
import FeaturesCalc.PreRetrievalCalc;
public class TimeEval
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
        List<Long> timesPostRetrievalFeatures = new ArrayList<>();
        PostRetrievalCalc pqCalc = new PostRetrievalCalc(req_readers[0],new EnglishAnalyzer(), new BM25Similarity());
        for(int i = 0; i < java_readers[0].numDocs();i++){
            long start = System.currentTimeMillis();
            pqCalc.getPostQueryFeatures(java_readers[0].document(i).getField("body").stringValue());
            timesPostRetrievalFeatures.add(System.currentTimeMillis()-start);
        }

        List<Long> timesPreRetrievalFeatures = new ArrayList<>();
        PreRetrievalCalc pc = new PreRetrievalCalc(req_readers[0],new EnglishAnalyzer());
        for(int i = 0; i < java_readers[0].numDocs();i++){
            long start = System.currentTimeMillis();
            pc.getPretrievalFeatures(java_readers[0].document(i).getField("body").stringValue());
            timesPreRetrievalFeatures.add(System.currentTimeMillis()-start);
        }

        double postretrievalAVG = timesPostRetrievalFeatures.stream().mapToLong(Long::longValue).sum()/((double) timesPostRetrievalFeatures.size());
        StandardDeviation sd = new StandardDeviation();
        double postretrievalSD = sd.evaluate(timesPostRetrievalFeatures.stream().mapToDouble(Long::doubleValue).toArray());

        double preretrievalAVG = timesPreRetrievalFeatures.stream().mapToLong(Long::longValue).sum()/((double) timesPreRetrievalFeatures.size());

        StandardDeviation sdpre= new StandardDeviation();
        double preretrievalSD = sdpre.evaluate(timesPreRetrievalFeatures.stream().mapToDouble(Long::doubleValue).toArray());
        System.out.println(postretrievalAVG);
        System.out.println(postretrievalSD);
        System.out.println(preretrievalAVG);
        System.out.println(preretrievalSD);

    }
}
