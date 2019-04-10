package biznasearch.search_engine;

import biznasearch.database.Getters;
import biznasearch.models.Business;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LuceneWrapper {
    private Analyzer analyzer;
    private String indexDir;
    private Connection dbConnection;


    public LuceneWrapper(String indexDir, Connection connection) {
        this.indexDir = indexDir;
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;
    }

    public List<Business> searchBusinesses(String queryText, int page, int maxResults) throws IOException, ParseException, SQLException {
        /* Search businesses using lucene */

        /* Build query */
        Query query = new QueryParser("business_name", analyzer).parse(queryText);

        /* Open business index TODO: Keep it open as a private field */
        Path path = Paths.get(indexDir, "businesses");
        Directory businessIndex = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(businessIndex);
        SpellCheckerIndexer check = new SpellCheckerIndexer(indexDir);
        String [] suggestions = check.getSimmilars(queryText,5);
        for (String suggestion : suggestions){
            System.err.println(suggestion);
        }

        /* Init results */
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, maxResults);

        /* Fetch the resulting business IDs */
        List<String> businessIDs = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            businessIDs.add(searcher.doc(scoreDoc.doc).get("business_id"));
        }

        if (businessIDs.size() == 0) {
            return new ArrayList<>();
        }
        /* Find businesses by their IDs and return them */
        return Getters.businessesByIDs(dbConnection, businessIDs);
    }

}
