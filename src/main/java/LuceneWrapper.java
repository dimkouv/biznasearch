import database.models.Business;
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

import javax.xml.soap.Text;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static database.Utilities.nextBusiness;

public class LuceneWrapper {
    Analyzer analyzer;
    Directory fsIndex;
    IndexWriterConfig indexWriterConfig;

    public LuceneWrapper(String indexPath) throws IOException {
        Path path = Paths.get(indexPath);

        analyzer = new StandardAnalyzer();
        fsIndex = FSDirectory.open(path);
        indexWriterConfig = new IndexWriterConfig(analyzer);

    }

    public void loadBusinesses(Connection connection) throws SQLException, IOException {
        IndexWriter iwriter;
        Business business;
        Document doc;

        iwriter = new IndexWriter(fsIndex, indexWriterConfig);

        String query = "SELECT * FROM businesses";
        PreparedStatement pst = connection.prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        business = nextBusiness(rs);
        while (business != null) {
            doc = new Document();

            doc.add(new Field("business_id", business.getId(), TextField.TYPE_STORED));

            if (business.getName() != null) {
                doc.add(new Field("business_name", business.getName(), TextField.TYPE_STORED));
            }

            if (business.getAddress() != null) {
                doc.add(new Field("business_address", business.getAddress(), TextField.TYPE_STORED));
            }

            iwriter.addDocument(doc);

            business = nextBusiness(rs);
        }

        iwriter.close();
    }

    public List<Document> searchBusinesses(String queryText) throws IOException, ParseException {
        Query query = new QueryParser("business_name", analyzer).parse(queryText);

        IndexReader ireader = DirectoryReader.open(fsIndex);
        IndexSearcher searcher = new IndexSearcher(ireader);
        TopDocs topDocs = searcher.search(query, 10);

        List<Document> docs = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            docs.add(searcher.doc(scoreDoc.doc));
        }

        return docs;
    }
}
