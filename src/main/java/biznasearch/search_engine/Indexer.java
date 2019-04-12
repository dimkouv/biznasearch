package biznasearch.search_engine;


import biznasearch.models.Business;
import biznasearch.models.Review;
import biznasearch.models.Tip;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static biznasearch.database.Parsers.*;
import static biznasearch.database.Shortcuts.*;

public class Indexer {
    private String indexDir;
    private String businessesIndexSuffix = "businesses";
    private String reviewsIndexSuffix = "reviews";
    private String tipsIndexSuffix = "tips";
    private Analyzer analyzer;
    private Connection dbConnection;

    public Indexer(String indexDir, Connection dbConnection) {
        this.analyzer = new StandardAnalyzer();
        this.indexDir = indexDir;
        this.dbConnection = dbConnection;
    }

    public void startIndexing(String city) throws SQLException, IOException {
        System.out.println(">>> Starting indexing, target city: " + city);
        indexBusinesses(city);
        indexTips(city);
        indexReviews(city);
    }

    private void indexBusinesses(String city) throws IOException, SQLException {
        Business business;
        Document businessEntry;

        Path path = Paths.get(indexDir, businessesIndexSuffix);
        Directory businessIndex = FSDirectory.open(path);

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter indexWriter = new IndexWriter(businessIndex, indexWriterConfig);

        String sqlQuery = sqlBusinessesOfCity(city);
        PreparedStatement pst = dbConnection.prepareStatement(sqlQuery);
        ResultSet rs = pst.executeQuery();

        System.out.println(">>> Starting business indexing");
        long startTime = System.currentTimeMillis();
        int cnt = 0;
        business = parseBusiness(rs);

        while (business != null) {
            cnt++;
            businessEntry = new Document();
            if (business.getId() != null) {
                businessEntry.add(new Field("business_id", business.getId(), TextField.TYPE_STORED));
            }

            if (business.getName() != null) {
                businessEntry.add(new Field("business_name", business.getName(), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(businessEntry);

            business = parseBusiness(rs);
        }

        indexWriter.close();
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf(">>> Indexed %d businesses in %s sec \n", cnt, elapsedTimeSec);
    }

    private void indexReviews(String city) throws IOException, SQLException {
        Review review;
        Document reviewEntry;
        Path path = Paths.get(indexDir, reviewsIndexSuffix);
        Directory reviewsIndex = FSDirectory.open(path);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(reviewsIndex, indexWriterConfig);

        String sqlQuery = sqlReviewsWhereCityIs(city);
        PreparedStatement pst = dbConnection.prepareStatement(sqlQuery);
        ResultSet rs = pst.executeQuery();

        System.out.println(">>> Starting reviews indexing");
        long startTime = System.currentTimeMillis();
        int cnt = 0;
        review = parseReview(rs);

        while (review != null) {
            cnt++;
            reviewEntry = new Document();

            if (review.getBusinessId() != null) {
                reviewEntry.add(new Field("review_business_id", review.getBusinessId(), TextField.TYPE_STORED));
            }

            if (review.getText() != null) {
                reviewEntry.add(new Field("review_text", review.getText(), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(reviewEntry);

            review = parseReview(rs);
        }

        indexWriter.close();
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf(">>> Indexed %d reviews in %s sec \n", cnt, elapsedTimeSec);
    }

    private void indexTips(String city) throws SQLException, IOException {
        Tip tip;
        Document tipEntry;
        Path path = Paths.get(indexDir, tipsIndexSuffix);
        Directory tipsIndex = FSDirectory.open(path);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(tipsIndex, indexWriterConfig);

        String sqlQuery = sqlTipsWhereCityIs(city);
        PreparedStatement pst = dbConnection.prepareStatement(sqlQuery);
        ResultSet rs = pst.executeQuery();

        System.out.println(">>> Starting tips indexing");
        long startTime = System.currentTimeMillis();
        int cnt = 0;
        tip = parseTip(rs);

        while (tip != null) {
            cnt++;
            tipEntry = new Document();

            if (tip.getBusinessId() != null) {
                tipEntry.add(new Field("tip_business_id", tip.getBusinessId(), TextField.TYPE_STORED));
            }

            if (tip.getText() != null) {
                tipEntry.add(new Field("tip_text", tip.getText(), TextField.TYPE_STORED));
            }

            indexWriter.addDocument(tipEntry);
            tip = parseTip(rs);
        }

        indexWriter.close();
        double elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.printf(">>> Indexed %d tips in %s sec \n", cnt, elapsedTimeSec);
    }
}
