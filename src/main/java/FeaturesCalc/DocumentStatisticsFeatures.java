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
    class DocumentStatistics{
        Set<String> termsUnique = new HashSet<>();
        int totalTermCount;
    }
    Map<String,DocumentStatistics> QueryDocTitleToStats = new HashMap<>();
    Map<String,DocumentStatistics> TargetDocTitleToStats = new HashMap<>();

    // #TODO extract Documentstatistics and calculate Features for the query.
    private DocumentStatisticsFeatures(){};

    public DocumentStatisticsFeatures(IndexReader readerQuery, IndexReader targetReader, Analyzer anal) throws IOException
    {
        for(int i = 0; i < readerQuery.numDocs();i++){
            Document doc = readerQuery.document(i);
            DocumentStatistics docStats = new DocumentStatistics();
            TokenStream ts = doc.getField("body").tokenStream(anal,null);
            ts.reset();
            while(ts.incrementToken()){
                docStats.termsUnique.add(ts.getAttribute(CharTermAttribute.class).toString());
                docStats.totalTermCount++;
            }
            ts.end();
            ts.close();
            QueryDocTitleToStats.put(doc.getField("title").stringValue(),docStats);
        }
        for(int i = 0; i < targetReader.numDocs();i++){
            Document doc = targetReader.document(i);
            DocumentStatistics docStats = new DocumentStatistics();
            TokenStream ts = doc.getField("body").tokenStream(anal,null);
            ts.reset();
            while(ts.incrementToken()){
                docStats.termsUnique.add(ts.getAttribute(CharTermAttribute.class).toString());
                docStats.totalTermCount++;
            }
            ts.end();
            ts.close();
            TargetDocTitleToStats.put(doc.getField("title").stringValue(),docStats);
        }
    }

    public double[] getDocumentStatisticsFeatures(String query, String target){
        double[] features = new double[5];
        features[0] = QueryDocTitleToStats.get(query).termsUnique.size();
        features[1] = QueryDocTitleToStats.get(query).totalTermCount;
        features[2] = TargetDocTitleToStats.get(target).termsUnique.size();
        features[3] = TargetDocTitleToStats.get(target).totalTermCount;
        Set<String> intersection = new HashSet<>(QueryDocTitleToStats.get(query).termsUnique);
        Set<String> union = new HashSet<>(QueryDocTitleToStats.get(query).termsUnique);
        intersection.retainAll(TargetDocTitleToStats.get(target).termsUnique);
        union.addAll((TargetDocTitleToStats.get(target).termsUnique));
        features[4] = intersection.size()/((double) union.size());
        return features;
    }
}
