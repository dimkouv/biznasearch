package biznasearch.search_engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import biznasearch.database.Getters;
import biznasearch.models.Business;
import biznasearch.models.SearchResult;

public class LuceneWrapper {
    private Analyzer analyzer;
    private Connection dbConnection;
    private String indexDir;
    private Directory businessIndex;
    private SpellChecker businessNameSpellChecker;

    public LuceneWrapper(String indexDir, Connection connection) throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;
        this.indexDir = indexDir;

        Path path = Paths.get(indexDir, "businesses");
        businessIndex = FSDirectory.open(path);
        businessNameSpellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexDir, "spell_check_business")));
    }

    public List<String> getBusinessNameSuggestions(String queryText, int maxResults) throws IOException {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(Arrays.asList(businessNameSpellChecker.suggestSimilar(queryText, maxResults)));

        return suggestions;
    }

    /**
     * This is the main search method. It splits the query text implementing the
     * ability to search by field. like field:queryText.
     *
     * @param queryText  The text the user gave to the interface.
     * @param resultsNum Number of results to fetch
     * @param orderBy    A businesses db field to order the results (prepend a minus
     *                   symbol to sort descending)
     * @return List of businesses' ids.
     * @throws ParseException
     * @throws SQLException
     * @throws IOException
     * @throws InvalidTokenOffsetsException
     */
    public List<SearchResult> search(String queryText, int resultsNum, String orderBy)
            throws ParseException, SQLException, IOException, InvalidTokenOffsetsException {

        List<SearchResult> results = new ArrayList<>();
        List<String> fields = Arrays.asList("name", "categories", "review", "tip");
        QueryParser queryParser;
        IndexReader businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        String[] querySplit = queryText.split(":");

        if (querySplit.length == 1) {
            queryText = querySplit[0];
            queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
        } else {
            if (!querySplit[1].matches("\\s+")) {
                querySplit[0] = querySplit[0].replaceAll("\\s+", "");
                if (!fields.contains(querySplit[0])) {
                    queryText = "";
                    for (String s : querySplit) {
                        queryText = queryText + " " + s;
                    }
                    queryText = queryText + "";
                    queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
                } else {
                    queryParser = new QueryParser(querySplit[0], new StandardAnalyzer());
                }
            } else {
                querySplit = queryText.replaceAll("\\s+", "").split(":");
                queryText = querySplit[0];
                queryParser = new MultiFieldQueryParser(fields.toArray(new String[fields.size()]), analyzer);
            }
        }

        Query query = queryParser.parse(queryText);
        System.out.println("Lucene Query: " + query.toString());

        TopDocs topDocs = searcher.search(query, resultsNum);
        List<String> businessIDs = new ArrayList<>();
        HashMap<String, String> businessHighlight = new HashMap<>();

        for (ScoreDoc top : topDocs.scoreDocs) {
            String businessID = searcher.doc(top.doc).get("id");
            String highlight = highLightQuery(query, top, queryParser.getField(), businessIndexReader, searcher);
            businessIDs.add(businessID);
            businessHighlight.put(businessID, highlight);
        }

        List<Business> businesses = Getters.businessesByIDs(dbConnection, businessIDs, orderBy);
        for (int i = 0; i < businesses.size(); i++) {
            Business business = businesses.get(i);
            String highlight = businessHighlight.get(business.getId());
            results.add(new SearchResult(business, highlight));
        }

        return results;
    }

    public String highLightQuery(Query query, ScoreDoc scoreDoc, String field, IndexReader indexReader,
            IndexSearcher searcher) throws IOException, InvalidTokenOffsetsException {
        if (field == null) {
            return "";
        }
        QueryScorer queryScorer = new QueryScorer(query);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
        Highlighter highlighter = new Highlighter(queryScorer);
        highlighter.setTextFragmenter(fragmenter);
        Document document = searcher.doc(scoreDoc.doc);
        String text = document.get(field);
        TokenStream tokenStream = TokenSources.getAnyTokenStream(indexReader, scoreDoc.doc, field, analyzer);
        String fragment = highlighter.getBestFragment(tokenStream, text);
        return fragment;
    }

    public Connection getDBConnection() {
        return dbConnection;
    }

    public String getIndexDir() {
        return indexDir;
    }
}
