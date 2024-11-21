package FeaturesCalc;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DocumentStatisticsFeatures
{
    /**
     * Class for calclating the DocumentStatistics Features
     */
    static class DocumentStatistics{
        /**
         * Datastructure for Documentsstatistics, containing unique Terms as Set and document length
         */
        Set<String> termsUnique = new HashSet<>();
        int totalTermCount;
    }
    Map<String,DocumentStatistics> queryDocTitleToStats = new HashMap<>();
    Map<String,DocumentStatistics> targetDocTitleToStats = new HashMap<>();

    private DocumentStatisticsFeatures(){};

    /**
     * Construct, which fills the maps with document stats
     * @param readerQuery IndexReader which contains source Artifacts
     * @param targetReader IndexReader which contains target Artifacts
     * @param anal Analyzer for preprocessing, must be the same used for the indices
     * @throws IOException thrown from Apache Lucene IndexReader or IndexSearcher
     */
    public DocumentStatisticsFeatures(IndexReader readerQuery, IndexReader targetReader, Analyzer anal) throws IOException
    {
        // Extract Document Statistics from reader
        for(int i = 0; i < readerQuery.numDocs();i++){
            Document doc = readerQuery.document(i);
            DocumentStatistics docStats = new DocumentStatistics();
            TokenStream ts = doc.getField("body").tokenStream(anal,null);
            ts.reset();
            // Extract Terms from TokenStream and add to stats
            while(ts.incrementToken()){
                docStats.termsUnique.add(ts.getAttribute(CharTermAttribute.class).toString());
                docStats.totalTermCount++;
            }
            ts.end();
            ts.close();
            queryDocTitleToStats.put(doc.getField("title").stringValue(),docStats);
        }
        // Extract Document Statistics from reader
        for(int i = 0; i < targetReader.numDocs();i++){
            Document doc = targetReader.document(i);
            DocumentStatistics docStats = new DocumentStatistics();
            TokenStream ts = doc.getField("body").tokenStream(anal,null);
            ts.reset();
            // Extract Terms from TokenStream and add to stats
            while(ts.incrementToken()){
                docStats.termsUnique.add(ts.getAttribute(CharTermAttribute.class).toString());
                docStats.totalTermCount++;
            }
            ts.end();
            ts.close();
            targetDocTitleToStats.put(doc.getField("title").stringValue(),docStats);
        }
    }

    /**
     * Calculates DocumentStatistitics Features for given query and target
     * @param query source document title
     * @param target target document title
     * @return features
     */
    public double[] getDocumentStatisticsFeatures(String query, String target){
        double[] features = new double[5];
        features[0] = queryDocTitleToStats.get(query).termsUnique.size();
        features[1] = queryDocTitleToStats.get(query).totalTermCount;
        features[2] = targetDocTitleToStats.get(target).termsUnique.size();
        features[3] = targetDocTitleToStats.get(target).totalTermCount;
        Set<String> intersection = new HashSet<>(queryDocTitleToStats.get(query).termsUnique);
        Set<String> union = new HashSet<>(queryDocTitleToStats.get(query).termsUnique);
        intersection.retainAll(targetDocTitleToStats.get(target).termsUnique);
        union.addAll((targetDocTitleToStats.get(target).termsUnique));
        features[4] = intersection.size()/((double) union.size());
        return features;
    }
}
