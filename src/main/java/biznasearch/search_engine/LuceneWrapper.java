package biznasearch.search_engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import biznasearch.database.Getters;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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

import biznasearch.models.Business;

public class LuceneWrapper {
    private Analyzer analyzer;
    private Connection dbConnection;

    private Directory businessIndex;
    private Directory index;
    private IndexReader businessIndexReader;

    private SpellChecker businessNameSpellChecker;

    public LuceneWrapper(String indexDir, Connection connection) throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;

        Path path = Paths.get(indexDir, "businesses");
        businessIndex = FSDirectory.open(path);
        Path indexPath = Paths.get(indexDir);
        index = FSDirectory.open(indexPath);
        businessNameSpellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDir, "spell_check_business_name")));
    }

    public List<String> getBusinessNameSuggestions(String queryText, int maxResults) throws IOException {
        List<String> suggestions = new ArrayList<String>();
        for (String s : businessNameSpellChecker.suggestSimilar(queryText, maxResults)) {
            suggestions.add(s);
        }

        return suggestions;
    }

    public List<Business> search (String queryText, int page, int maxResults) throws ParseException, SQLException, IOException {
        String [] query = queryText.split(":");
        List <String> searchRes;
        switch (query[0]) {
            case "name":
                searchRes = searchBusinesses(query[1], page, maxResults);
                break;
            case "category":
                searchRes = searchBusinessesByCategory(query[1], page, maxResults);
                break;
            case "tip":
                searchRes =searchByTips(query[1], page, maxResults);
                break;
            case "review":
                searchRes = searchByReviews(query[1], page, maxResults);
                break;
            default:
                searchRes = searchInAll(query[0], page, maxResults);
                break;
        }
        return fetchSearch((ArrayList<String>) searchRes);
    }

    public List<Business> fetchSearch(ArrayList<String> list) throws SQLException {
        return Getters.businessesByIDs(dbConnection, list);
    }

    public List<String> searchInAll(String queryText, int page, int maxResults) throws IOException, ParseException, SQLException {
        List <String> res = new ArrayList<>();
        List <String> names = searchBusinesses(queryText, page, maxResults);
        res.addAll(names);
        res.addAll(searchBusinessesByCategory(queryText, page, maxResults/4));
        res.addAll(searchByTips(queryText, page, maxResults/4));
        res.addAll(searchByReviews(queryText, page, maxResults/4));

        return res;
    }

    private List<String> searchByReviews(String queryText, int page, int i) {
        return new ArrayList<>();
    }

    private List<String> searchByTips(String queryText, int page, int maxResults) {
        List <String> res = new ArrayList<>();

        return res;
    }

    private List<String> searchBusinessesByCategory(String queryText, int page, int maxResults) throws ParseException, IOException {
        List <String> res = new ArrayList<>();
        Query query = new QueryParser("categories", analyzer).parse(queryText);
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        TopDocs topDocs = searcher.search(query, maxResults);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            res.add(searcher.doc(scoreDoc.doc).get("id"));
        }

        if (res.size() == 0) {
            return new ArrayList<>();
        }

        return res;

    }


    public List<String> searchBusinesses(String queryText, int page, int maxResults)
            throws IOException, ParseException, SQLException {

        Query query = new QueryParser("name", analyzer).parse(queryText);

        businessIndexReader = DirectoryReader.open(businessIndex);

        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        TopDocs topDocs = searcher.search(query, maxResults);

        /* Fetch the resulting business IDs */
        List<String> businessIDs = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            businessIDs.add(searcher.doc(scoreDoc.doc).get("id"));
        }

        if (businessIDs.size() == 0) {
            return new ArrayList<>();
        }

        return businessIDs;
        /* Find businesses by their IDs from db and return them */
//        return Getters.businessesByIDs(dbConnection, businessIDs);
    }

}
