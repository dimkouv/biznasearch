package biznasearch.search_engine;

import static biznasearch.database.Shortcuts.sqlBusinessesIdxColsOfCity;
import static biznasearch.database.Shortcuts.sqlReviewsIdxColsWhereCityIs;
import static biznasearch.database.Shortcuts.sqlTipsIdxColsWhereCityIs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
    private String indexDir;
    private Analyzer analyzer;
    private Connection dbConnection;

    public Indexer(String indexDir, Connection dbConnection) {
        this.analyzer = new StandardAnalyzer();
        this.indexDir = indexDir;
        this.dbConnection = dbConnection;
    }

    /**
     * Acts as a wrapper for @createIndex method. Calls create index for all the
     * required fields.
     * 
     * @param city The target city to index businesses from.
     * @throws SQLException
     * @throws IOException
     */
    public void startIndexing(String city) throws SQLException, IOException {
        System.out.println(">>> Starting indexing, target city: " + city);

        createIndex("businesses", Arrays.asList("id", "name"), sqlBusinessesIdxColsOfCity(city));
        createIndex("reviews", Arrays.asList("business_id", "text"), sqlReviewsIdxColsWhereCityIs(city));
        createIndex("tips", Arrays.asList("business_id", "text"), sqlTipsIdxColsWhereCityIs(city));
    }

    /**
     * Generates lucene index for columns of a target database table.
     * 
     * @param targetTable The target database table
     * @param columns     The columns of the table to index, those columns will be
     *                    searchable after indexing.
     * @param sqlQuery    The sql query to fetch the target rows.
     */
    private void createIndex(String targetTable, List<String> columns, String sqlQuery)
            throws SQLException, IOException {
        Document docEntry;
        Path path = Paths.get(indexDir, targetTable);
        Directory businessIndex = FSDirectory.open(path);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(businessIndex, indexWriterConfig);

        PreparedStatement pst = dbConnection.prepareStatement(sqlQuery);
        pst.setFetchSize(100);
        ResultSet rs = pst.executeQuery();

        long startTime = System.currentTimeMillis();
        int cnt = 0;

        System.out.println(">>> Starting " + targetTable + " indexing");
        while (rs.next()) {
            docEntry = new Document();
            for (int i = 0; i < columns.size(); i++) {
                docEntry.add(new Field(columns.get(i), rs.getString(i + 1), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(docEntry);
            cnt++;
        }

        indexWriter.close();
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf("\tindexed " + cnt + " entries in " + elapsedTimeSec + "sec", elapsedTimeSec);
    }
}
