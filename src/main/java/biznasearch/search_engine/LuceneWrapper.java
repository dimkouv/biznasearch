package biznasearch.search_engine;

import biznasearch.database.Getters;
import biznasearch.models.Business;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public List<Business> search (String queryText, int page, int maxResults, String orderBy) throws ParseException, SQLException, IOException {
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
        TopDocs topDocs = searcher.search(query, maxResults);
        
        for (ScoreDoc top: topDocs.scoreDocs){
            searchResults.add(searcher.doc(top.doc).get("id"));
        }
        
        return Getters.businessesByIDs(dbConnection, (ArrayList <String>) searchResults, orderBy);
    }

    public Connection getDBConnection() {
        return dbConnection;
    }

    public String getIndexDir() {
        return indexDir;
    }
}
