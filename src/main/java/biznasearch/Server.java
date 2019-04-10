package biznasearch;

import biznasearch.controllers.BusinessControllers;
import biznasearch.search_engine.Indexer;
import biznasearch.search_engine.LuceneWrapper;
import biznasearch.search_engine.SpellCheckerIndexer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import spark.Spark;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static spark.Spark.before;
import static spark.Spark.get;

public class Server {
    private LuceneWrapper luc;
    private SpellCheckerIndexer check;
    private int port;
    private String city;
    private String indexDir;
    private Connection dbConnection;

    private Server(String dbUrl, String dbUsername, String dbPassword, String indexDir, int port, String city) throws SQLException {
        this.dbConnection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        this.indexDir = indexDir;
        this.port = port;
        this.city = city;

        luc = new LuceneWrapper(indexDir, this.dbConnection);
        check = new SpellCheckerIndexer(indexDir);
    }

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.ERROR);

            /* Load properties */
            Properties props = new Properties();
            InputStream input = new FileInputStream("src/main/resources/application.properties");
            props.load(input);

            Server server = new Server(
                    props.getProperty("url"),
                    props.getProperty("username"),
                    props.getProperty("password"),
                    props.getProperty("indexDir"),
                    Integer.parseInt(props.getProperty("port")),
                    props.getProperty("city")
            );

            server.registerRoutesAndStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerRoutesAndStart() throws IOException {
        System.out.println("Server running on http://127.0.0.1:" + port);
        Properties props = new Properties();
        InputStream input = new FileInputStream("src/main/resources/application.properties");
        props.load(input);
        Spark.port(port);
        Spark.staticFiles.location("/web");
        this.enableCors();

        /*
         * GET /businesses
         *
         * GET PARAMETERS
         * --------------
         * query - A query for the businesses
         */
        get("/businesses", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = {"query"};

            for (String param : requiredParameters) {
                if (req.queryParams(param) == null) {
                    res.status(400);
                    return "{\"message\":\"'" + param + "' wasn't found in parameters.\"}";
                }
            }

            if (req.queryParams("query").length() == 0) {
                res.status(400);
                return "{\"message\":\"'query' is empty.\"}";
            }

            return BusinessControllers.businessSearch(req.queryParams("query"), 0, luc, 10);
        });

        /*
         * GET /start-indexing
         *
         * GET PARAMETERS
         * --------------
         * server-token - A token used for authentication
         */
        get("/start-indexing", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = {"server-token"};

            for (String param : requiredParameters) {
                if (req.queryParams(param) == null) {
                    res.status(400);
                    return "{\"message\":\"'" + param + "' wasn't found in parameters.\"}";
                }
            }

            if (!req.queryParams("server-token").equals(props.getProperty("token"))) {
                res.status(400);
                return "{\"message\":\"Authentication error.\"}";
            }

            Thread indexJob = new Thread(() -> {
                try {
                    Indexer indexer = new Indexer(indexDir, dbConnection);
                    indexer.startIndexing(city);
                    check.spellIndexBusinessName();
                } catch (SQLException | IOException v) {
                    System.out.println(v);
                }
            });
            indexJob.start();

            return "{\"message\":\"Indexing operation started...\"}";

        });
    }

    // Enables CORS on requests. This method is an initialization method and should be called once.
    private void enableCors() {
        Spark.staticFiles.header("Access-Control-Allow-Origin", "*");

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.type("application/json");
        });
    }
}
