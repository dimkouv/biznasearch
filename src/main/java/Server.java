import database.ConnectionManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Server {
    private ConnectionManager cm;
    private LuceneWrapper luc;

    public Server() throws SQLException, IOException {
        cm = new ConnectionManager(
                "jdbc:postgresql://192.168.0.10:5432/biznasearch",
                "sysdba",
                "masterkey"
        );

        luc = new LuceneWrapper("/tmp/testindex");
        luc.loadBusinesses(cm.getConnection());
    }

    private void searchBusinesses(String query) throws IOException, ParseException {
        long start = System.currentTimeMillis();
        List<Document> docs = luc.searchBusinesses(query);
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        System.out.println(">>> " + docs.size() + " results in " + elapsedTimeMillis + "ms");

        for (Document doc: docs) {
            System.out.println(doc.get("business_name") + " " + doc.get("business_id"));
        }
    }

    public void simulate() throws IOException, ParseException {
        Scanner inp = new Scanner(System.in);

        String query = "";
        while (!query.equals("/exit")) {
            System.out.print("Give query or /exit: ");
            query = inp.nextLine();
            if (!query.equals("/exit")) {
                this.searchBusinesses(query);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.simulate();

            server.stop();
        } catch (SQLException | IOException | ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    public void stop() throws SQLException {
        cm.closeConnection();
    }
}
