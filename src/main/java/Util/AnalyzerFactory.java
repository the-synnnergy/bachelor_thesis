package Util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import java.util.List;

public class AnalyzerFactory
{
    CharArraySet stopwords;
    String language;

    public AnalyzerFactory(List<String> stopwordList, String language)
    {
        stopwords = new CharArraySet(stopwordList,true);
        this.language = language;
    }

    public Analyzer getAnalyzer()
    {
        switch (language)
        {
            case "EN":
                return new EnglishAnalyzer(stopwords);
        }
        throw new UnsupportedOperationException("Language not available");
    }
}
