package biznasearch.search_engine;

import static org.apache.lucene.search.highlight.TokenSources.getAnyTokenStream;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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
import org.apache.lucene.search.highlight.*;
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
     * @param page Page size
     * @param maxResults Number of max results the top docs will return.
     * @param orderBy A businesses db field to order the results (prepend a minus symbol to sort descending)
     * @return List of businesses' ids.
     * @throws ParseException
     * @throws SQLException
     * @throws IOException
     */
    public List<Business> search (String queryText, int page, int maxResults, String orderBy) throws ParseException, SQLException, IOException, InvalidTokenOffsetsException {
        List <String> searchResults = new ArrayList<>(); 
        List <String> fields = Arrays.asList("name", "categories", "review", "tip");
        
        QueryParser queryParser;
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        String querySplit []  = queryText.split(":");
        
        if (querySplit.length==1){
            queryText = querySplit[0];
            queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
        }else{
            if (!querySplit[1].matches("\\s+")){
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
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(formatter,new QueryScorer(query));
        TopDocs topDocs = searcher.search(query, maxResults);
        
        for (ScoreDoc top: topDocs.scoreDocs){
            searchResults.add(searcher.doc(top.doc).get("id"));
            highlight(top.doc, searcher, businessIndexReader, queryParser.getField() , highlighter);
        }
        
        return Getters.businessesByIDs(dbConnection, searchResults, orderBy);
    }

    public void highlight(int docId, IndexSearcher searcher, IndexReader indexReader, String field, Highlighter highlighter) throws IOException, InvalidTokenOffsetsException {
        Document doc = searcher.doc(docId);
        String text = doc.get(field);
        TokenStream tokenStream = getAnyTokenStream(indexReader, docId, field, analyzer);
        TextFragment  [] frag = highlighter.getBestTextFragments(tokenStream, text, false, 4);
        float maxScore = 0;
        TextFragment maxFrag = null ;
        for (TextFragment f: frag){
            System.out.println("x");
            if (f != null && f.getScore()!=0){
                if (f.getScore() > maxScore){
                    maxScore = f.getScore();
                    maxFrag = f ;
                }
            }
        }
        // System.out.println("score: " + maxFrag.getScore() + ", frag: " + (maxFrag.toString()));

    }

    public Connection getDBConnection() {
        return dbConnection;
    }

<<<<<<< Updated upstream
    public String getIndexDir() {
        return indexDir;
    }
=======

>>>>>>> Stashed changes
}
