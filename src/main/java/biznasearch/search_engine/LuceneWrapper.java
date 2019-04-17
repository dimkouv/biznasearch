package biznasearch.search_engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import biznasearch.database.Getters;
import biznasearch.models.Business;

public class LuceneWrapper {
    private Analyzer analyzer;
    private Connection dbConnection;

    private Directory businessIndex;
    private IndexReader businessIndexReader;

    private SpellChecker businessNameSpellChecker;

    public LuceneWrapper(String indexDir, Connection connection) throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;

        Path path = Paths.get(indexDir, "businesses");
        businessIndex = FSDirectory.open(path);

        businessNameSpellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDir, "spell_check_business_name")));
    }

    public List<String> getBusinessNameSuggestions(String queryText, int maxResults) throws IOException {
        List<String> suggestions = new ArrayList<String>();
        for (String s : businessNameSpellChecker.suggestSimilar(queryText, maxResults)) {
            suggestions.add(s);
        }

        return suggestions;
    }

    public List<Business> searchBusinesses(String queryText, int page, int maxResults)
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

        /* Find businesses by their IDs from db and return them */
        return Getters.businessesByIDs(dbConnection, businessIDs);
    }

}
