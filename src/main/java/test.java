//import weka.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.index.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class test {

    public static void main(String[] args) throws Exception {
        index_test();
        //lucene_main(args[0]);
        /*Tokenizer sample = new AlphabeticTokenizer();
        String test_string = null;
        try{
            Path path_to = Path.of(args[0]);
            test_string = Files.readString(path_to);
        }catch(Exception e){
            System.exit(-1);
        };
        sample.tokenize(test_string);
        ArrayList<String> words = new ArrayList<>();
        SortedSet<String> unique_words = new TreeSet<String>();
        while(sample.hasMoreElements()){
            words.add(sample.nextElement());
        }
        Stemmer stemmer = new PorterStemmer();
        ArrayList<String> stemmed_words = new ArrayList<>();
        for(String tmp : words){
            System.out.println("Unstemmed:"+" "+tmp);
            System.out.println("Stemmed:"+" "+stemmer.stem(tmp));
            unique_words.add((String) stemmer.stem(tmp));
        }
        Iterator<String> it = unique_words.iterator();*/
        /*while (it.hasNext()){
            System.out.println(it.next());
        }*/

    };

    public static void lucene_main(String file) throws IOException {
        Analyzer anal = new EnglishAnalyzer();
        String test_string = null;
        try{
            Path path_to = Path.of(file);
            test_string = Files.readString(path_to);
        }catch(Exception e){
            System.exit(-1);
        };
        TokenStream tokens = anal.tokenStream("lol",test_string);
        tokens.reset();
        CharTermAttribute attr = tokens.addAttribute(CharTermAttribute.class);
        while(tokens.incrementToken()){
            System.out.println(attr.toString());
        }
        CharArraySet test = EnglishAnalyzer.getDefaultStopSet();
        System.out.println(test.toString());
        //Query weird = new MatchAllDocsQuery();
    }

    public static void index_test() throws Exception{
        File index = new File("index");
        FSDirectory directory = FSDirectory.open(index.toPath());
        EnglishAnalyzer analyzer = new EnglishAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        //writerConfig.setSimilarity(new TFIDFSimilarity());
        IndexWriter writer = new IndexWriter(directory,writerConfig);
        File f = new File("mini_newsgroups/comp.sys.ibm.pc.hardware");
        String[] pathnames = f.list();
        for(String name : pathnames){
            Document doc = new Document();
            String tmp = Files.readString(Path.of("mini_newsgroups/comp.sys.ibm.pc.hardware/" + name));
            FieldType field = new FieldType();
            field.setTokenized(true);
            field.setStored(true);
            field.setStoreTermVectors(true);
            field.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            doc.add(new Field("body", tmp, field));
            doc.add(new Field("name", name, field));
            writer.addDocument(doc);
            List<IndexableField> fields = doc.getFields();


            for (IndexableField fiellld: fields){
                //System.out.println(fiellld.name());
                //System.out.println(fiellld.stringValue());
            }
        }
        writer.commit();
        IndexReader reader = DirectoryReader.open(writer);
        for(int i = 0; i< reader.maxDoc();i++){
            Terms t = reader.getTermVector(i, "body");
            TermsEnum t_it = t.iterator();
            System.out.println(t_it.term().utf8ToString());
            while(t_it.next() != null){
                //System.out.println(t_it.term().utf8ToString());
            }
            //System.out.println("Ende");
        }
        IndexSearcher search = new IndexSearcher(reader);
        //search.setSimilarity(new ClassicSimilarity());
        EnglishAnalyzer en = new EnglishAnalyzer();
        QueryBuilder qb = new QueryBuilder(en);
        Query q = qb.createBooleanQuery("body", "video video");
        // Create Query with Should from text
        /*Query q = qb.createBooleanQuery("body", "Path: cantaloupe.srv.cs.cmu.edu!das-news.harvard.edu!ogicse!emory!swrinde!sdd.hp.com!nigel.msen.com!fmsrl7!glang\n" +
                "From: glang@slee01.srl.ford.com (Gordon Lang)\n" +
                "Newsgroups: comp.sys.ibm.pc.hardware\n" +
                "Subject: Please help identify video hardware\n" +
                "Message-ID: <1pqep5INN88e@fmsrl7.srl.ford.com>\n" +
                "Date: 5 Apr 93 23:19:01 GMT\n" +
                "Article-I.D.: fmsrl7.1pqep5INN88e\n" +
                "Organization: Ford Motor Company Research Laboratory\n" +
                "Lines: 11\n" +
                "NNTP-Posting-Host: slee01.srl.ford.com\n" +
                "X-Newsreader: Tin 1.1 PL5\n" +
                "\n" +
                "I need a device (either an ISA board or a subsystem) which will\n" +
                "take two RGB video signals and combine them according to a template.\n" +
                "The template can be as simple as a rectangular window with signal\n" +
                "one being used for the interior and signal two for the exterior.\n" +
                "But I beleive fancier harware may also exist which I do not want\n" +
                "to exclude from my search.  I know this sort of hardware exists\n" +
                "for NTSC, etc. but I need it for RGB.\n" +
                "\n" +
                "Please email and or post any leads....\n" +
                "\n" +
                "Gordon Lang (glang@smail.srl.ford.com  -or-  glang@holo6.srl.ford.com)\n");*/
        TopDocs top = search.search(q, Integer.MAX_VALUE);
        System.out.println(search.getSimilarity());
        System.out.println(top.totalHits);
        ScoreDoc[] scoreDocs = top.scoreDocs;
        for (ScoreDoc scored : scoreDocs){
            int docId = scored.doc;
            float luceneScore = scored.score;
            Document doc = search.doc(docId);
            System.out.println(luceneScore+" "+doc.get("name"));
        }
        //System.out.println(search.explain(q,scoreDocs[1].doc));
        Terms t = reader.getTermVector(0, "body");
        TermsEnum te = t.iterator();
        te.next();
        te.next();
        te.next();
        System.out.println(te.docFreq());
        System.out.println(te.totalTermFreq());
        System.out.println(te.term().utf8ToString());
        PostingsEnum pe = null;
        PostingsEnum pe1 = te.postings(null, PostingsEnum.FREQS);
        System.out.println(te.term().utf8ToString());
        int doc = pe1.nextDoc();
        System.out.println(pe1.freq());
        System.out.println("docid: "+doc+" "+reader.document(doc).get("name"));
        writer.close();
        directory.close();

    }
}
