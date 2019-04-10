package biznasearch;

import biznasearch.controllers.BusinessControllers;
import biznasearch.search_engine.LuceneWrapper;
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
    private int port;

    private Server(String dbUrl, String dbUsername, String dbPassword, String indexDir, int port) throws SQLException {
        /* Open database connection */
        Connection con = DriverManager.getConnection(
                dbUrl, dbUsername, dbPassword
        );

        this.port = port;

        /* Start indexing */
        luc = new LuceneWrapper(indexDir, con);
    }

    public static void main(String[] args) {
        try {
            BasicConfigurator.configure();
            Logger.getRootLogger().setLevel(Level.ERROR);

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

            luc.startIndexing();
            return "{\"message\":\"Indexing completed.\"}";
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
