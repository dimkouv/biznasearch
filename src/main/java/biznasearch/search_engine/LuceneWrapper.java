package biznasearch.search_engine;

import biznasearch.database.Getters;
import biznasearch.models.Business;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
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
import java.util.Collections;
import java.util.List;

public class LuceneWrapper {
    private Analyzer analyzer;
    private Connection dbConnection;

    private Directory businessIndex;
    private Directory tipsIndex;
    private Directory reviewsIndex;

    private IndexReader businessIndexReader;
    private IndexReader tipsIndexReader;
    private IndexReader reviewsIndexReader;

    private SpellChecker businessNameSpellChecker;
    private static int bus = 1;
    private static int tips = 2;
    private static int reviews = 3;
    private static int cat = 4;

    public LuceneWrapper(String indexDir, Connection connection) throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;

        Path path = Paths.get(indexDir, "businesses");
        Path tipsPath = Paths.get(indexDir, "tips");
        Path reviewsPath = Paths.get(indexDir, "reviews");


        businessIndex = FSDirectory.open(path);
        tipsIndex = FSDirectory.open(tipsPath);
        reviewsIndex = FSDirectory.open(reviewsPath);

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
     * @return List of businesses' ids.
     * @throws ParseException
     * @throws SQLException
     * @throws IOException
     */
    public List<Business> search (String queryText, int page, int maxResults) throws ParseException, SQLException, IOException {
        String queryTextClean = queryText.replaceAll("\\s+","");
        String [] query = queryTextClean.split(":");
        List <String> searchRes;
        if (query.length == 2){
            switch (query[0]) {
                case "name":
                    searchRes = searchBusinesses(query[1], page, maxResults);
                    break;
                case "category":
                    searchRes = searchBusinessesByCategory(query[1], page, maxResults);
                    break;
                case "tip":
                    searchRes = searchByTips(query[1], page, maxResults);
                    break;
                case "review":
                    searchRes = searchByReviews(query[1], page, maxResults);
                    break;
                default:
                    String ques = query[0] + " " + query[1];
                    searchRes = searchInAll(ques, page, maxResults);
                    break;
            }
        }else{
            searchRes = searchInAll(query[0], page, maxResults);
        }
        return fetchSearch((ArrayList<String>) searchRes);
    }

    /**
     * Connects Searcher to Database Parser.
     * @param list
     * @return List<Businesses>
     * @throws SQLException
     */
    public List<Business> fetchSearch(ArrayList<String> list) throws SQLException {
        return Getters.businessesByIDs(dbConnection, list);
    }

    /**
     * Uses field to find the correct column to search in the database from the top Docs.
     * @param query
     * @param maxResults
     * @param searcher
     * @param field
     * @return List<String>
     * @throws IOException
     */
    public List<String> findSearch(Query query, int maxResults, IndexSearcher searcher, int field) throws IOException {
        List <String> res = new ArrayList<>();
        TopDocs topDocs = searcher.search(query, maxResults);
        if (field == bus) {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                res.add(searcher.doc(scoreDoc.doc).get("id"));
            }
        }else if (field == cat) {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs){
                res.add(searcher.doc(scoreDoc.doc).get("id"));
            }
        }else{
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                res.add(searcher.doc(scoreDoc.doc).get("business_id"));
            }
        }
        if (res.size() == 0) {
            return new ArrayList<>();
        }
        return res;
    }

    /**
     * Implements the search in all possible fields when the field is not specified
     * @param queryText
     * @param page
     * @param maxResults
     * @return List of all business ids that the query text exists in any of the fields.
     * @throws IOException
     * @throws ParseException
     */
    public List<String> searchInAll(String queryText, int page, int maxResults) throws IOException, ParseException {
        List <String> res = new ArrayList<>();
        res.addAll(searchBusinesses(queryText, page, maxResults));
        res.addAll(searchBusinessesByCategory(queryText, page, maxResults));
        res.addAll(searchByTips(queryText, page, maxResults));
        res.addAll(searchByReviews(queryText, page, maxResults));

        return res;
    }

    /**
     * Searches the reviews' text for the query.
     * @param queryText
     * @param page
     * @param maxResults
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private List<String> searchByReviews(String queryText, int page, int maxResults) throws IOException, ParseException {
        Query query = new QueryParser("text", analyzer).parse(queryText);
        reviewsIndexReader = DirectoryReader.open(reviewsIndex);
        IndexSearcher searcher = new IndexSearcher(reviewsIndexReader);
        return findSearch(query, maxResults, searcher, reviews);
    }

    /**
     * Searches tips' text for the query.
     * @param queryText
     * @param page
     * @param maxResults
     * @return
     * @throws ParseException
     * @throws IOException
     */
    private List<String> searchByTips(String queryText, int page, int maxResults) throws ParseException, IOException {
        Query query = new QueryParser("text", analyzer).parse(queryText);
        tipsIndexReader = DirectoryReader.open(tipsIndex);
        IndexSearcher searcher = new IndexSearcher(tipsIndexReader);
        return findSearch(query, maxResults, searcher, tips);

    }

    /**
     * Searches business categories for the query.
     * @param queryText
     * @param page
     * @param maxResults
     * @return
     * @throws ParseException
     * @throws IOException
     */
    private List<String> searchBusinessesByCategory(String queryText, int page, int maxResults) throws ParseException, IOException {
        Query query = new QueryParser("categories", analyzer).parse(queryText);
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        return findSearch(query, maxResults, searcher, cat);
    }

    /**
     * Searches business names for the query.
     * @param queryText
     * @param page
     * @param maxResults
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public List<String> searchBusinesses(String queryText, int page, int maxResults)
            throws IOException, ParseException {
        Query query = new QueryParser("name", analyzer).parse(queryText);
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        return findSearch(query, maxResults, searcher, bus);
    }

}
