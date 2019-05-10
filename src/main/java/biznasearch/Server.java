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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import biznasearch.controllers.BusinessControllers;
import biznasearch.database.Shortcuts;
import biznasearch.search_engine.Indexer;
import biznasearch.search_engine.LuceneWrapper;
import spark.Spark;

public class Server {
    private LuceneWrapper luc;
    private int port;
    private String city;
    private String indexDir;
    private Connection dbConnection;

    private Server(String dbUrl, String dbUsername, String dbPassword, String indexDir, int port, String city)
            throws SQLException, IOException {
        this.dbConnection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        this.indexDir = indexDir;
        this.port = port;
        this.city = city;

        luc = new LuceneWrapper(indexDir, this.dbConnection);
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
                    Integer.parseInt(props.getProperty("port")), props.getProperty("city"));

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
         * GET PARAMETERS -------------- query - A query for the businesses
         */
        get("/businesses", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = { "query" };

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

            List<String> acceptedOrderCols = Arrays.asList(
                "review_count", "-review_count", "stars", "-stars", "clicks", "-clicks", "");
            if (!acceptedOrderCols.contains(req.queryParams("orderBy"))) {
                res.status(404);
                return "{\"message\":\"'orderBy="+req.queryParams("orderBy")+"' is not valid.\"}";
            }

            Thread queryLogJob = new Thread(() -> {
                try {
                    Shortcuts.sqlLogQuery(dbConnection, req.queryParams("query"));
                } catch (SQLException v) {
                    System.out.println(v);
                    v.printStackTrace();
                }
            });
            queryLogJob.start();

            return BusinessControllers.businessSearch(req.queryParams("query"), 0, luc, 10, req.queryParams("orderBy"));
        });

        /*
         * GET /suggest
         *
         * GET PARAMETERS -------------- query - A query to find similars
         */
        get("/suggest", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = { "query" };

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

            return BusinessControllers.businessNameSimilars(req.queryParams("query"), luc, 10);
        });

        /*
         * GET /start-indexing
         *
         * GET PARAMETERS -------------- authtoken - A token used for authentication
         */
        get("/index", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = { "token" };

            for (String param : requiredParameters) {
                if (req.queryParams(param) == null) {
                    res.status(400);
                    return "{\"message\":\"'" + param + "' wasn't found in parameters.\"}";
                }
            }

            if (!req.queryParams("token").equals(props.getProperty("token"))) {
                res.status(400);
                return "{\"message\":\"Authentication error.\"}";
            }

            Thread indexJob = new Thread(() -> {
                try {
                    Indexer indexer = new Indexer(indexDir, dbConnection);
                    indexer.startIndexing(city);
                } catch (SQLException | IOException v) {
                    System.out.println(v);
                    v.printStackTrace();
                }
            });
            indexJob.start();

            return "{\"message\":\"Indexing operation started...\"}";
        });

        /*
         * POST /click
         * 
         * Called when a user clicks on a business that appears in the results.
         *
         * POST PARAMETERS -------------- business-id - ID of the business to increment its clicks
         */
        post("/click", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = { "business-id" };

            for (String param : requiredParameters) {
                if (req.queryParams(param) == null) {
                    res.status(400);
                    return "{\"message\":\"'" + param + "' wasn't found in parameters.\"}";
                }
            }

            Thread statsJob = new Thread(() -> {
                try {
                    String businessID = req.queryParams("business-id");
                    if (businessID != null && businessID.length() == 22) {
                        Shortcuts.sqlAddBusinessClick(dbConnection, businessID);
                    }
                } catch (SQLException v) {
                    System.out.println(v);
                    v.printStackTrace();
                }
            });
            statsJob.start();

            return "{\"message\":\"OK\"}";
        });

        /*
         * GET /query-suggest
         *
         * GET PARAMETERS -------------- query - A query to find similars
         */
        get("/query-suggest", (req, res) -> {
            res.type("application/json");
            String[] requiredParameters = { "query" };

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

            return BusinessControllers.querySimilars(req.queryParams("query"), luc, 10);
        });
    }

    // Enables CORS on requests. This method is an initialization method and should
    // be called once.
    private void enableCors() {
        Spark.staticFiles.header("Access-Control-Allow-Origin", "*");

        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "*");
            res.type("application/json");
        });
    }
}
