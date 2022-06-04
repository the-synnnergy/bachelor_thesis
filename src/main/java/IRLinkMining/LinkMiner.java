package IRLinkMining;

import Util.AnalyzerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinkMiner
{
    IndexSearcher searcher;
    AnalyzerFactory analyzerFactory;
    IndexReader reader;
    public LinkMiner(IndexReader reader, AnalyzerFactory analyzerFactory)
    {
        this.reader = reader;
        searcher = new IndexSearcher(this.reader);
        this.analyzerFactory = analyzerFactory;
    }

    public List<String> getLinksByAbsolute(String query, int numDocsToRetrieve) throws IOException
    {
        // anal = analyzerFactory.getAnalyzer();
        Analyzer anal = new EnglishAnalyzer();
        QueryBuilder qb = new QueryBuilder(anal);
        Query luceneQuery = qb.createBooleanQuery("body",query);
        TopDocs topDocs = searcher.search(luceneQuery,numDocsToRetrieve);
        List<String> results = new ArrayList<>();
        for(ScoreDoc scoreDoc : topDocs.scoreDocs)
        {
            results.add(reader.document(scoreDoc.doc).getField("title").stringValue());
        }
        return results;
    }


    public List<String> getLinksByRelative(String query, float percentageOfTopScore) throws IOException
    {
        // anal = analyzerFactory.getAnalyzer();
        Analyzer anal = new EnglishAnalyzer();
        QueryBuilder qb = new QueryBuilder(anal);
        Query luceneQuery = qb.createBooleanQuery("body",query);
        TopDocs topDocs = searcher.search(luceneQuery,Integer.MAX_VALUE);
        List<String> results = new ArrayList<>();
        float topScore = topDocs.scoreDocs[0].score;
        for(ScoreDoc scoreDoc : topDocs.scoreDocs)
        {
            if(topScore * percentageOfTopScore > scoreDoc.score) continue;
            results.add(reader.document(scoreDoc.doc).getField("title").stringValue());
        }
        return results;
    }
}
