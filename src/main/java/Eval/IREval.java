package Eval;

import FeaturesCalc.FeaturesCalc;
import com.opencsv.CSVWriter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static IndexTools.IndexCreator.createIndices;
import static Parser.iTrustParser.get_NameFileMap;
import static Parser.iTrustParser.get_true_req_to_source_links;

public class IREval
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


        int[] numberOfDocsToRetrieve = new int[]{30,40,50,60,70,100,150,200}; // ,
        double[] percentageToRetrieve = new double[]{0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1};
        List<String[]> csvDataNumberDocs = new ArrayList<>();
        List<String[]> csvDataPercentage = new ArrayList<>();
        for(int i : numberOfDocsToRetrieve)
        {
            csvDataNumberDocs.add(evaluateIRDocWise(i,req_readers, java_readers));
        }
        for(double i : percentageToRetrieve)
        {
            csvDataPercentage.add(evaluateIRPercentageWise(i,req_readers,java_readers));
        }
        // evaluate different scores!
        // evaluate for 30 , 40 , 50 , 60 , 70 , 100 , 150 , 200 Documents
        // evaluate for 90, 80, 70 , 60 , 40 , 50 , 30 , 20 , 10 %
        File numDocsFile = new File("numberDocs.csv");
        File percentageFile = new File("percentage.csv");
        FileWriter numDocsFileout = new FileWriter(numDocsFile);
        FileWriter percentageFileout = new FileWriter(percentageFile);
        CSVWriter numDocsCSVWriter = new CSVWriter(numDocsFileout);
        CSVWriter percentageCSVWriter = new CSVWriter(percentageFileout);
        numDocsCSVWriter.writeAll(csvDataNumberDocs);
        percentageCSVWriter.writeAll(csvDataPercentage);
        numDocsCSVWriter.close();
        percentageCSVWriter.close();
    }

    private static String[] evaluateIRPercentageWise(double percentageToRetrieve, IndexReader[] reader_req, IndexReader[] java_readers) throws IOException, ParserConfigurationException, SAXException
    {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        List<Map<String,List<String>>> retrievedDocsForAllSims = new ArrayList<>();
        IndexSearcher searcher[] = new IndexSearcher[java_readers.length];
        for(int i = 0; i <java_readers.length;i++)
        {
            searcher[i] = new IndexSearcher(java_readers[i]);
        }
        searcher[1].setSimilarity(new ClassicSimilarity());
        searcher[2].setSimilarity(new LMDirichletSimilarity());
        searcher[3].setSimilarity(new LMJelinekMercerSimilarity(0.7f));
        for(int i = 0; i < reader_req.length;i++)
        {
            Map<String,List<String>> retrievedDocs = new HashMap<>();

            for(int j = 0; j < reader_req[i].numDocs();j++)
            {
                List<String> extractedResults = new ArrayList<>();
                QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
                Query q= qb.createBooleanQuery("body",reader_req[i].document(j).getField("body").stringValue());
                TopDocs tops = searcher[i].search(q,Integer.MAX_VALUE);
                double minScore = tops.scoreDocs[0].score * percentageToRetrieve;
                for(ScoreDoc scoreDoc : tops.scoreDocs)
                {
                    if(scoreDoc.score < minScore) continue;
                    int docId = scoreDoc.doc;
                    extractedResults.add(java_readers[i].document(docId).getField("title").stringValue());
                }
                retrievedDocs.put(reader_req[i].document(j).getField("title").stringValue(),extractedResults);
            }
            retrievedDocsForAllSims.add(retrievedDocs);
        }
        List<Double> results = getMetrics(retrievedDocsForAllSims);
        List<String> data = new ArrayList<>();
        data.add(String.valueOf(percentageToRetrieve));
        for(Double result: results)
        {
            data.add(String.valueOf(result));
        }
        return data.toArray(new String[0]);

    }

    private static String[] evaluateIRDocWise(int num, IndexReader[] reader_req, IndexReader[] java_readers) throws IOException, ParserConfigurationException, SAXException
    {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        List<Map<String,List<String>>> retrievedDocsForAllSims = new ArrayList<>();
        IndexSearcher searcher[] = new IndexSearcher[java_readers.length];
        for(int i = 0; i <java_readers.length;i++)
        {
            searcher[i] = new IndexSearcher(java_readers[i]);
        }
        searcher[1].setSimilarity(new ClassicSimilarity());
        searcher[2].setSimilarity(new LMDirichletSimilarity());
        searcher[3].setSimilarity(new LMJelinekMercerSimilarity(0.7f));
        for(int i = 0; i < reader_req.length;i++)
        {
            Map<String,List<String>> retrievedDocs = new HashMap<>();

            for(int j = 0; j < reader_req[i].numDocs();j++)
            {
                List<String> extractedResults = new ArrayList<>();
                QueryBuilder qb = new QueryBuilder(new EnglishAnalyzer());
                Query q= qb.createBooleanQuery("body",reader_req[i].document(j).getField("body").stringValue());
                TopDocs tops = searcher[i].search(q,Integer.MAX_VALUE);
                int maxDocs = Math.min(tops.scoreDocs.length, num);
                for(int k = 0; k < maxDocs; k++)
                {
                    int docId = tops.scoreDocs[k].doc;
                    extractedResults.add(java_readers[i].document(docId).getField("title").stringValue());
                }
                retrievedDocs.put(reader_req[i].document(j).getField("title").stringValue(),extractedResults);
            }
            retrievedDocsForAllSims.add(retrievedDocs);
        }
        List<Double> results = getMetrics(retrievedDocsForAllSims);
        List<String> data = new ArrayList<>();
        data.add(String.valueOf(num));
        for(Double result: results)
        {
           data.add(String.valueOf(result));
        }
        return data.toArray(new String[0]);
    }

    private static List<Double> getMetrics(List<Map<String,List<String>>> retrievedDocsForAllSims) throws ParserConfigurationException, IOException, SAXException
    {
        Map<String, List<String>> links = get_true_req_to_source_links("/home/marcel/Downloads/SMOS/", "/home/marcel/Downloads/SMOS/answer_req_code.xml");
        List<Double> metrics = new ArrayList<>();
        for(Map<String,List<String>> docsPerSim : retrievedDocsForAllSims)
        {
            double recall = 0;
            double precision = 0;
            for(Map.Entry<String, List<String>> entry : docsPerSim.entrySet())
            {
                List<String> linkList = links.get(entry.getKey());
                List<String> linksInIrRetrieved = entry.getValue().stream().filter(linkList::contains).collect(Collectors.toList());
                recall += linksInIrRetrieved.size()/ ((double)linkList.size());
                precision += linksInIrRetrieved.size()/ ((double)entry.getValue().size());
            }
            recall = recall/docsPerSim.size();
            precision = precision/ docsPerSim.size();
            metrics.add(recall);
            metrics.add(precision);
        }
        return metrics;
    }
}
