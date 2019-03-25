package biznasearch;

import biznasearch.controllers.BusinessControllers;
import biznasearch.search_engine.LuceneWrapper;

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

    private Server() throws IOException, SQLException {
        /* Load properties */
        Properties props = new Properties();
        InputStream input = new FileInputStream("src/main/resources/application.properties");
        props.load(input);

        /* Open database connection */
        Connection con = DriverManager.getConnection(
                props.getProperty("url"), props.getProperty("username"), props.getProperty("password")
        );

        /* Start indexing */
        luc = new LuceneWrapper(props.getProperty("indexDir"), con);
        luc.startIndexing();
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.registerRoutesAndStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerRoutesAndStart() {

        /*
         * GET /businesses
         *
         * GET PARAMETERS
         * --------------
         * query - A query for the businesses
         */
        get("/businesses", (req, res) -> {
            res.type("application/json");
            return BusinessControllers.businessSearch(req.queryParams("query"), 0, luc, 10);
        });
    }
}
