package biznasearch;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import biznasearch.controllers.ServerControllers;
import biznasearch.search_engine.LuceneWrapper;
import spark.Spark;

public class Server {
    private LuceneWrapper luc;
    private int port;
    private String city;
    private String authToken;
    private String domain;

    private Server(String dbUrl, String dbUsername, String dbPassword, String indexDir, int port, String city,
            String authToken, String domain) throws SQLException, IOException {
        Connection dbConnection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        this.port = port;
        this.city = city;
        this.authToken = authToken;
        this.domain = domain;

        luc = new LuceneWrapper(indexDir, dbConnection);
    }

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.ERROR);

            /* Load properties */
            Properties props = new Properties();
            InputStream input = new FileInputStream("src/main/resources/application.properties");
            props.load(input);

            Server server = new Server(props.getProperty("url"), props.getProperty("username"),
                    props.getProperty("password"), props.getProperty("indexDir"),
                    Integer.parseInt(props.getProperty("port")), props.getProperty("city"), props.getProperty("token"),
                    props.getProperty("domain"));

            server.registerRoutesAndStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerRoutesAndStart() {
        System.out.println("Server running on http://" + domain + ":" + port);
        Spark.port(port);
        Spark.staticFiles.location("/web");
        this.enableCors();

        ServerControllers controllers = new ServerControllers(luc, authToken, city);

        // GET /search
        // Performs business search and returns the results in json.
        //
        // Parameters:
        // query (string) - The lucene query to execute
        // results-num (int) - Number of results
        // order-by (string) - The lucene query to execute
        get("/search", controllers::getSearchResults);

        // GET /spell-check
        // Performs spell check and returns suggestions.
        //
        // Parameters:
        // query (string) - The lucene query to execute
        get("/spell-check", controllers::getQuerySpellSuggestions);

        // POST /index
        // Performs indexing operation.
        //
        // Parameters:
        // token (string) - Authentication token (defined in properties)
        post("/index", controllers::startIndexing);

        // POST /click
        // Adds a new click to a business.
        //
        // Parameters:
        // business-id (string) - ID of the business to increment the clicks
        post("/click", controllers::logClick);

        // POST /query-suggest
        // Suggest a query, ideally called when user types
        //
        // Parameters:
        // query (string) - The target query
        get("/query-suggest", controllers::getQuerySuggestions);
    }

    // Enables CORS on requests. This method is an initialization method and should
    // be called once.
    private void enableCors() {
        Spark.staticFiles.header("Access-Control-Allow-Origin", domain);

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", domain);
            res.header("Access-Control-Allow-Headers", domain);
        });
    }
}
