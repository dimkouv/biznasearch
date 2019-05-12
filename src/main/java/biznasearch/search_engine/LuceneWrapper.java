package biznasearch.search_engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import biznasearch.database.Getters;
import biznasearch.models.Business;


public class LuceneWrapper {
    private Analyzer analyzer;
    private Connection dbConnection;
    private String indexDir;
    private Directory businessIndex;
    private IndexReader businessIndexReader;
    private SpellChecker businessNameSpellChecker;

    public LuceneWrapper(String indexDir, Connection connection) throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;
        this.indexDir = indexDir;

        Path path = Paths.get(indexDir, "businesses");
        businessIndex = FSDirectory.open(path);
        businessNameSpellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDir, "spell_check_business_name")));

    }

    public List<String> getBusinessNameSuggestions(String queryText, int maxResults) throws IOException {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(Arrays.asList(businessNameSpellChecker.suggestSimilar(queryText, maxResults)));

        return suggestions;
    }

    /**
     * This is the main search method.
     * It splits the query text implementing the ability to search by field.
     * like field:queryText.
     * @param queryText The text the user gave to the interface.
     * @param resultsNum Number of results to fetch
     * @param orderBy A businesses db field to order the results (prepend a minus symbol to sort descending)
     * @return List of businesses' ids.
     * @throws ParseException
     * @throws SQLException
     * @throws IOException
     */
    public List<Business> search (String queryText, int resultsNum, String orderBy) throws ParseException, SQLException, IOException, InvalidTokenOffsetsException {
        List <String> searchResults = new ArrayList<>(); 
        List <String> fields = Arrays.asList("name", "categories", "review", "tip");
        List <String> highlightedText = new ArrayList<>();
        List <String> fieldToSearch = new ArrayList<>();
        QueryParser queryParser;
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        String querySplit []  = queryText.split(":");
        
        if (querySplit.length==1){
            queryText = querySplit[0];
            queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
        }else{
            if (!querySplit[1].matches("\\s+")){
                querySplit[0] = querySplit[0].replaceAll("\\s+","");
                if (!fields.contains(querySplit[0])){
                    queryText = "";
                    for (String s: querySplit){
                        queryText = queryText +" "+ s;
                    }
                    queryText = queryText +"";
                    queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);    
                }else{
                    queryParser = new QueryParser(querySplit[0], new StandardAnalyzer());
                }
            }else{
                querySplit = queryText.replaceAll("\\s+", "").split(":");
                queryText = querySplit[0];
                queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
            }
        }
        
        Query query = queryParser.parse(queryText);
        System.out.println("Type of query: " + query.getClass().getSimpleName());
        System.out.println("And the query is: "+ query.toString());
        String splitField[] = query.toString().split("([+-:\\s]+)");
        for (String f: splitField){
            if (fields.contains(f)){
                fieldToSearch.add(f);
            }
        }
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter();
        
        TopDocs topDocs = searcher.search(query, resultsNum);
        QueryScorer queryScorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter,queryScorer);
        highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, 200));
        
        for (ScoreDoc top: topDocs.scoreDocs){
            searchResults.add(searcher.doc(top.doc).get("id"));
            highlightedText = highlight(top.doc, searcher, businessIndexReader, fieldToSearch , highlighter);
            System.out.println(highlightedText);
        }
        
        return Getters.businessesByIDs(dbConnection, searchResults, orderBy);
    }

    public List<String> highlight(int docId, IndexSearcher searcher, 
        IndexReader indexReader, List<String> field, Highlighter highlighter) 
            throws IOException, InvalidTokenOffsetsException {
        
        List <String> fragments = new ArrayList<>();
        if (field== null){
            System.err.println(">>> NULL ARRAYLIST PARAM");
            return new ArrayList<>();
        }
        Document doc = searcher.doc(docId);
        for (int i =0; i<field.size();i++){
            String text = doc.get(field.get(i));
            fragments.add(highlighter.getBestFragment(analyzer, field.get(i), text));
        }

        return fragments;

    }

    public Connection getDBConnection() {
        return dbConnection;
    }

    public String getIndexDir() {
        return indexDir;
    }
}
