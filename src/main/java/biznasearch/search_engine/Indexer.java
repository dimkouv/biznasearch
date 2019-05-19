package biznasearch.search_engine;

import static biznasearch.database.Shortcuts.sqlBusinessesIdxColsOfCity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import biznasearch.database.Shortcuts;

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
     */
    public void startIndexing(String city) throws SQLException, IOException {
        System.out.println(">>> Starting indexing, target city: " + city);

        createBusinessIndex(city);
        createBusinessNameSpellIndex();
    }

    /**
     * Generates lucene index for columns of a target database table.
     * 
     * @param targetTable The target database table
     * @param columns     The columns of the table to index, those columns will be
     *                    searchable after indexing.
     * @param sqlQuery    The sql query to fetch the target rows.
     */
    private void createBusinessIndex(String city) throws SQLException, IOException {
        Document docEntry;
        Path path = Paths.get(indexDir, "businesses");
        Directory businessIndex = FSDirectory.open(path);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(businessIndex, indexWriterConfig);

        String sqlQuery = sqlBusinessesIdxColsOfCity(city);
        List<String> columns = Arrays.asList("id", "name", "categories");

        PreparedStatement pst = dbConnection.prepareStatement(sqlQuery);
        pst.setFetchSize(100);
        ResultSet rs = pst.executeQuery();

        long startTime = System.currentTimeMillis();
        int cnt = 0;

        System.out.println(">>> Starting indexing");
        while (rs.next()) {
            docEntry = new Document();
            for (int i = 0; i < columns.size(); i++) {
                docEntry.add(new Field(columns.get(i), rs.getString(i + 1), TextField.TYPE_STORED));
            }

            String sqlReviews = Shortcuts.sqlReviewsIdxColsWhereBusinessIdIs(rs.getString(1));
            PreparedStatement pstRevs = dbConnection.prepareStatement(sqlReviews);
            pstRevs.setFetchSize(100);
            ResultSet rsRevs = pstRevs.executeQuery();
            while (rsRevs.next()) {
                docEntry.add(new Field("review_id", rsRevs.getString(1), TextField.TYPE_STORED));
                docEntry.add(new Field("review", rsRevs.getString(2), TextField.TYPE_STORED));
            }

            String sqlTips = Shortcuts.sqlTipsIdxColsWhereBusinessIdIs(rs.getString(1));
            PreparedStatement pstTips = dbConnection.prepareStatement(sqlTips);
            pstTips.setFetchSize(100);
            ResultSet rsTips = pstTips.executeQuery();
            while (rsTips.next()) {
                docEntry.add(new Field("tip_id", rsTips.getString(1), TextField.TYPE_STORED));
                docEntry.add(new Field("tip", rsTips.getString(2), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(docEntry);
            cnt++;

            if (cnt % 1000 == 0) {
                double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
                System.out.println("\tIndexed " + cnt + " businesses in " + elapsedTimeSec + "sec");
            }
        }

        indexWriter.close();
        businessIndex.close();
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.println("\tIndexed " + cnt + " businesses in " + elapsedTimeSec + "sec");
    }

    /**
     * Generates spell checking dictionary for business names.
     */
    public void createBusinessNameSpellIndex() throws IOException {
        List<String> spellFields = new ArrayList<>(Arrays.asList("name", "tip", "reviews", "categories"));

        Directory businessesIndexDir = FSDirectory.open(Paths.get(indexDir, "businesses"));
        DirectoryReader businessIndexReader = DirectoryReader.open(businessesIndexDir);

        for (String field : spellFields) {
            long startTime = System.currentTimeMillis();
            System.out.println(">>> Starting business." + field + " spell check indexing");

            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
            Directory spellCheckDir = FSDirectory.open(Paths.get(indexDir, "spell_check_business"));

            LuceneDictionary dictionary = new LuceneDictionary(businessIndexReader, field);
            SpellChecker spell = new SpellChecker(spellCheckDir);
            spell.indexDictionary(dictionary, indexWriterConfig, false);
            spell.close();

            spellCheckDir.close();
            double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
            System.out.println("\tCompleted in " + elapsedTimeSec + "sec");
        }

        businessIndexReader.close();
        businessesIndexDir.close();
    }
}
