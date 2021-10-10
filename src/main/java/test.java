//import weka.*;
import FeaturesCalc.PreQueryCalc;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
//import org.apache.lucene.core.*;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static FeaturesCalc.Util.get_document_vectors;

public class test {


    public static void PreQueryTest() throws Exception{
        File f = new File("mini_newsgroups/comp.sys.ibm.pc.hardware");
        String[] pathname = f.list();
        List<ImmutablePair<String, String>> documents = new ArrayList<>();
        for (String name : pathname) {
            String tmp = Files.readString(Path.of("mini_newsgroups/comp.sys.ibm.pc.hardware/" + name));
            documents.add(new ImmutablePair<String, String>(name, tmp));
        }
        File f2 = new File("mini_newsgroups/comp.sys.mac.hardware");
        String[] pathname2 = f2.list();
        List<ImmutablePair<String, String>> documents2 = new ArrayList<>();
        for (String name : pathname2) {
            String tmp = Files.readString(Path.of("mini_newsgroups/comp.sys.mac.hardware/" + name));
            documents2.add(new ImmutablePair<String, String>(name, tmp));
        }
        PreQueryCalc pre1 = new PreQueryCalc(documents);
        Map<String,PreQueryCalc.PrequeryFeatures> features = new HashMap<>();
        for(ImmutablePair<String,String> doc : documents2){
            features.put(doc.left,pre1.get_prequery_features(doc.right));
        }
        for (Map.Entry<String, PreQueryCalc.PrequeryFeatures> entry : features.entrySet()){
            System.out.println(entry.getValue().toString());
        }
    }
    public static void main(String[] args) throws Exception {
        //PreQueryTest();
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
        /*File f = new File("mini_newsgroups/comp.sys.ibm.pc.hardware");
        String[] pathname = f.list();
        List<ImmutablePair<String, String>> documents = new ArrayList<>();
        for (String name : pathname) {
            String tmp = Files.readString(Path.of("mini_newsgroups/comp.sys.ibm.pc.hardware/" + name));
            documents.add(new ImmutablePair<String, String>(name, tmp));
        }
        FeaturesCalc.SimilarityCalc calc = new FeaturesCalc.SimilarityCalc(documents, FeaturesCalc.Similarities.TF_IDF);
        Map<String,Float> results = calc.getCalculatedSimilarities("Path: cantaloupe.srv.cs.cmu.edu!das-news.harvard.edu!ogicse!emory!swrinde!sdd.hp.com!nigel.msen.com!fmsrl7!glang\\n\" +\n" +
                "                \"From: glang@slee01.srl.ford.com (Gordon Lang)\\n\" +\n" +
                "                \"Newsgroups: comp.sys.ibm.pc.hardware\\n\" +\n" +
                "                \"Subject: Please help identify video hardware\\n\" +\n" +
                "                \"Message-ID: <1pqep5INN88e@fmsrl7.srl.ford.com>\\n\" +\n" +
                "                \"Date: 5 Apr 93 23:19:01 GMT\\n\" +\n" +
                "                \"Article-I.D.: fmsrl7.1pqep5INN88e\\n\" +\n" +
                "                \"Organization: Ford Motor Company Research Laboratory\\n\" +\n" +
                "                \"Lines: 11\\n\" +\n" +
                "                \"NNTP-Posting-Host: slee01.srl.ford.com\\n\" +\n" +
                "                \"X-Newsreader: Tin 1.1 PL5\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"I need a device (either an ISA board or a subsystem) which will\\n\" +\n" +
                "                \"take two RGB video signals and combine them according to a template.\\n\" +\n" +
                "                \"The template can be as simple as a rectangular window with signal\\n\" +\n" +
                "                \"one being used for the interior and signal two for the exterior.\\n\" +\n" +
                "                \"But I beleive fancier harware may also exist which I do not want\\n\" +\n" +
                "                \"to exclude from my search.  I know this sort of hardware exists\\n\" +\n" +
                "                \"for NTSC, etc. but I need it for RGB.\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"Please email and or post any leads....\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"Gordon Lang (glang@smail.srl.ford.com  -or-  glang@holo6.srl.ford.com)\\n");
        results.forEach((k,v)->System.out.println(k+" "+v));*/
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
        writerConfig.setSimilarity(new LMJelinekMercerSimilarity(0.7f));
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
            Field fd = new Field("body", tmp, field);
            doc.add(fd);
            //System.out.println(fd.stringValue());
            doc.add(new StringField("name", name, Field.Store.YES));
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
            //System.out.println(t_it.term().utf8ToString());
            while(t_it.next() != null){
                //System.out.println(t_it.term().utf8ToString());
            }
            //System.out.println("Ende");
        }
        IndexSearcher search = new IndexSearcher(reader);
        search.setSimilarity(new ClassicSimilarity());
        EnglishAnalyzer en = new EnglishAnalyzer();
        QueryBuilder qb = new QueryBuilder(en);
        List<Integer> docs = new ArrayList<>();
        docs.add(57);
        Query q = new PertubedQuery(new Term("body", "video"),docs);
        // Create Query with Should from text
        /** Query q = qb.createBooleanQuery("body", "Path: cantaloupe.srv.cs.cmu.edu!das-news.harvard.edu!ogicse!emory!swrinde!sdd.hp.com!nigel.msen.com!fmsrl7!glang\n" +
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
                "Gordon Lang (glang@smail.srl.ford.com  -or-  glang@holo6.srl.ford.com)\n"); **/
        TopDocs top = search.search(q, Integer.MAX_VALUE);
        //System.out.println(search.getSimilarity());
        //System.out.println(top.totalHits);
        ScoreDoc[] scoreDocs = top.scoreDocs;
        for (ScoreDoc scored : scoreDocs){
            int docId = scored.doc;
            float luceneScore = scored.score;
            Document doc = search.doc(docId);
            //System.out.println(luceneScore+" "+ docId);
            //System.out.println(search.explain(q, docId));
        }
        //System.out.println(search.explain(q,scoreDocs[1].doc));
        /*Iterator<BooleanClause> qt = q.iterator();
        while (qt.hasNext()){
            BooleanClause bc = qt.next();
            TermQuery bq = (TermQuery) bc.getQuery();
            System.out.println(bq.getTerm().text());
        }
        Terms t = MultiTerms.getTerms(reader, "body");
        TermsEnum te = t.iterator();*/
        /** te.next();
        te.next();
        te.next();
        System.out.println(te.docFreq());
        System.out.println(te.totalTermFreq());
        System.out.println(te.term().utf8ToString()); */
        /*PostingsEnum pe = null;
        PostingsEnum pe1 = te.postings(null, PostingsEnum.FREQS);
        //System.out.println(te.term().utf8ToString());
        //int doc = pe1.nextDoc();
        //System.out.println(pe1.freq());
        //System.out.println("docid: "+doc+" "+reader.document(doc).get("name"));
        while(te.next() != null){
            //System.out.println(te.term().utf8ToString());
        }*/
        Terms all = MultiTerms.getTerms(reader,"body");
        TermsEnum all_enum = all.iterator();
        while (all_enum.next() != null){
            //System.out.println(all_enum.term().utf8ToString());
        }
        Map<Integer,int[]> test_map = get_document_vectors(reader);
        int[] test_int = test_map.get(0);
        System.out.println(reader.document(0).getField("body").stringValue());
        System.out.println("Term Vectors now");
        System.out.println(test_int[1685]);
        writer.close();
        directory.close();

    }
}
