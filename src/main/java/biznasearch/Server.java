package biznasearch;

import biznasearch.controllers.BusinessControllers;
import biznasearch.search_engine.LuceneWrapper;
import spark.Spark;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static spark.Spark.get;

public class Server {
    private LuceneWrapper luc;
    private int port;

    private Server(String dbUrl, String dbUsername, String dbPassword, String indexDir, int port) throws IOException, SQLException {
        /* Open database connection */
        Connection con = DriverManager.getConnection(
                dbUrl, dbUsername, dbPassword
        );

        this.port = port;

        /* Start indexing */
        luc = new LuceneWrapper(indexDir, con);
        luc.startIndexing();
    }

    public static void main(String[] args) {
        try {
            /* Load properties */
            Properties props = new Properties();
            InputStream input = new FileInputStream("src/main/resources/application.properties");
            props.load(input);

            Server server = new Server(props.getProperty("url"),
                    props.getProperty("username"),
                    props.getProperty("password"),
                    props.getProperty("indexDir"),
                    Integer.parseInt(props.getProperty("port")));

            server.registerRoutesAndStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerRoutesAndStart() {
        Spark.port(port);
        /*
         * GET /businesses
         *
         * GET PARAMETERS
         * --------------
         *
         *
         * query - A query for the businesses
         */
        get("/businesses", (req, res) -> { //"show.hmtl");
            res.type("application/json");
            return BusinessControllers.businessSearch(req.queryParams("query"), 0, luc, 10);
        });
    }
}
