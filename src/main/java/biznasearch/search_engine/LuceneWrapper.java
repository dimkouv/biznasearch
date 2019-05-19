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
    public List<Business> search(String queryText, int resultsNum, String orderBy)
            throws ParseException, SQLException, IOException, InvalidTokenOffsetsException {

        List<String> searchResults = new ArrayList<>();
        List<String> fields = Arrays.asList("name", "categories", "review", "tip");
        QueryParser queryParser;
        businessIndexReader = DirectoryReader.open(businessIndex);
        IndexSearcher searcher = new IndexSearcher(businessIndexReader);
        String querySplit[] = queryText.split(":");

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
        System.out.println("Type of query: " + query.getClass().getSimpleName());
        System.out.println("And the query is: " + query.toString());

        TopDocs topDocs = searcher.search(query, resultsNum);

        for (ScoreDoc top : topDocs.scoreDocs) {
            searchResults.add(searcher.doc(top.doc).get("id"));
            highLightQuery(query, top, queryParser.getField(), businessIndexReader, searcher);
        }

        return Getters.businessesByIDs(dbConnection, searchResults, orderBy);
    }

    public String highLightQuery(Query query, ScoreDoc scoreDoc, String field, IndexReader indexReader,
            IndexSearcher searcher) throws IOException, InvalidTokenOffsetsException {
        String docId="";
        String review_id="";
        if (field == null){
            System.out.println(">>> Null field param");
            return "";
        }
        QueryScorer queryScorer = new QueryScorer(query);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
        Highlighter highlighter = new Highlighter(queryScorer);
        highlighter.setTextFragmenter(fragmenter);
        Document document = searcher.doc(scoreDoc.doc);
        docId= document.get("id");
        review_id = document.get("review_id");
        System.out.println("docId: "+docId+"\nreview_id: "+review_id);
        String text =document.get(field);
        TokenStream tokenStream = TokenSources.getAnyTokenStream(indexReader, scoreDoc.doc, field, analyzer);
        String fragment = highlighter.getBestFragment(tokenStream, text);
        System.out.println("The fragment is: "+fragment);
        return fragment;

    }
    
    public Connection getDBConnection() {
        return dbConnection;
    }

    public String getIndexDir() {
        return indexDir;
    }
}
