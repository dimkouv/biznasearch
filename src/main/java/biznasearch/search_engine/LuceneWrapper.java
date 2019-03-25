package biznasearch.search_engine;

import biznasearch.database.Getters;
import biznasearch.models.Business;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static biznasearch.database.Parsers.parseBusiness;

public class LuceneWrapper {
    Directory businessIndexReader;
    private Analyzer analyzer;
    private String indexDir;
    private Connection dbConnection;

    public LuceneWrapper(String indexDir, Connection connection) {
        this.indexDir = indexDir;
        this.analyzer = new StandardAnalyzer();
        this.dbConnection = connection;
    }

    public void startIndexing() throws SQLException, IOException {
        //indexBusinesses();
    }

    private void indexBusinesses() throws IOException, SQLException {
        /* Add Businesses from database to Lucene index. */

        Business business;
        Document businessEntry;

        /* Open/create index file */
        Path path = Paths.get(indexDir, "businesses");
        Directory businessIndex = FSDirectory.open(path);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(businessIndex, indexWriterConfig);

        /* Prepare SQL query */
        String query = "SELECT * FROM businesses";
        PreparedStatement pst = dbConnection.prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        /* Index businesses fields */
        business = parseBusiness(rs);
        while (business != null) {
            businessEntry = new Document();

            if (business.getId() != null) {
                businessEntry.add(new Field("business_id", business.getId(), TextField.TYPE_STORED));
            }

            if (business.getName() != null) {
                businessEntry.add(new Field("business_name", business.getName(), TextField.TYPE_STORED));
            }

            if (business.getAddress() != null) {
                businessEntry.add(new Field("business_address", business.getAddress(), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(businessEntry);
            business = parseBusiness(rs);
        }

        indexWriter.close();
    }

    public List<Business> searchBusinesses(String queryText, int page, int maxResults) throws IOException, ParseException, SQLException {
        /* Search businesses using lucene */

        /* Build query */
        Query query = new QueryParser("business_name", analyzer).parse(queryText);

        /* Open business index TODO: Keep it open as a private field */
        Path path = Paths.get(indexDir, "businesses");
        Directory businessIndex = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(businessIndex);

        /* Init results */
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, maxResults);

        /* Fetch the resulting business IDs */
        List<String> businessIDs = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            businessIDs.add(searcher.doc(scoreDoc.doc).get("business_id"));
        }

        if (businessIDs.size() == 0) {
            return new ArrayList<Business>();
        }
        /* Find businesses by their IDs and return them */
        return Getters.businessesByIDs(dbConnection, businessIDs);
    }
}
