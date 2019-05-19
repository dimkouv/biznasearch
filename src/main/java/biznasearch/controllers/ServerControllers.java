package biznasearch.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import biznasearch.models.SearchResult;
import com.google.gson.Gson;

import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

import biznasearch.database.Getters;
import biznasearch.database.Shortcuts;
import biznasearch.models.Business;
import biznasearch.models.Query;
import biznasearch.search_engine.Indexer;
import biznasearch.search_engine.LuceneWrapper;
import spark.Request;
import spark.Response;

public class ServerControllers {
    private LuceneWrapper luc;
    private String authToken;
    private String targetCity;

    private long totalResponseTimeMs;
    private int totalRequests;

    public ServerControllers(LuceneWrapper luc, String authToken, String targetCity) {
        this.luc = luc;
        this.authToken = authToken;
        this.targetCity = targetCity;
    }

    private void asJSON(Response res) {
        // sets the response content-type header to json
        res.type("application/json");
    }

    private boolean missingParamsExist(Request req, String[] requiredParams) {
        // checks if any missing parameter exists
        for (String param : requiredParams) {
            if (req.queryParams(param) == null) {
                return true;
            }
        }
        return false;
    }

    private String jsonMessage(String message) {
        return "{\"message\": \"" + message + "\"}";
    }

    private void log(Request req, long startTime) {
        long elapsedTimeMillis = System.currentTimeMillis() - startTime;
        totalResponseTimeMs += elapsedTimeMillis;
        totalRequests++;

        System.out.println(String.format("(%dms) [%s] %s\n", elapsedTimeMillis, req.requestMethod(), req.pathInfo()));
    }

    public String getSearchResults(Request req, Response res) throws SQLException,
            org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidTokenOffsetsException {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        if (missingParamsExist(req, new String[] { "query", "order-by", "results-num" })) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Query is empty");
        }

        List<String> acceptedOrderCols = Arrays.asList("review_count", "-review_count", "stars", "-stars", "clicks",
                "-clicks", "");

        if (!acceptedOrderCols.contains(req.queryParams("order-by"))) {
            res.status(404);
            log(req, startTime);
            return jsonMessage("order-by value is not valid");
        }

        Thread queryLogJob = new Thread(() -> {
            try {
                Shortcuts.sqlLogQuery(luc.getDBConnection(), req.queryParams("query"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        queryLogJob.start();

        int resultsNum = Integer.parseInt(req.queryParams("results-num"));
        List<SearchResult> results = luc.search(req.queryParams("query"), resultsNum, req.queryParams("order-by"));
        String json = new Gson().toJson(results);
        log(req, startTime);
        return json;
    }

    public String getQuerySpellSuggestions(Request req, Response res) throws IOException {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        if (missingParamsExist(req, new String[] { "query" })) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Query is empty");
        }

        List<String> similars = luc.getBusinessNameSuggestions(req.queryParams("query"), 10);
        String json = new Gson().toJson(similars);
        log(req, startTime);
        return json;
    }

    public String startIndexing(Request req, Response res) {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        if (missingParamsExist(req, new String[] { "token" })) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Some required parameters are missing");
        }

        if (!req.queryParams("token").equals(authToken)) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Authentication error");
        }

        Thread indexJob = new Thread(() -> {
            try {
                Indexer indexer = new Indexer(luc.getIndexDir(), luc.getDBConnection());
                indexer.startIndexing(targetCity);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        });
        indexJob.start();

        String json = jsonMessage("Indexing operation started");
        log(req, startTime);
        return json;
    }

    public String logClick(Request req, Response res) {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        if (missingParamsExist(req, new String[] { "business-id" })) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Some required parameters are missing");
        }

        Thread statsJob = new Thread(() -> {
            try {
                String businessID = req.queryParams("business-id");
                if (businessID != null && businessID.length() == 22) {
                    Shortcuts.sqlAddBusinessClick(luc.getDBConnection(), businessID);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        statsJob.start();

        String json = jsonMessage("logged");
        log(req, startTime);
        return json;
    }

    public String getQuerySuggestions(Request req, Response res) throws SQLException {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        if (missingParamsExist(req, new String[] { "query" })) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            log(req, startTime);
            return jsonMessage("Query is empty");
        }

        List<Query> similars = Getters.similarQueries(luc.getDBConnection(), req.queryParams("query"), 10);
        String json = new Gson().toJson(similars);
        log(req, startTime);
        return json;
    }

    public String getServerStatistics(Request req, Response res) {
        long startTime = System.currentTimeMillis();
        asJSON(res);

        HashMap<String, String> stats = new HashMap<String, String>();

        if (totalRequests == 0) {
            stats.put("average_response_time", "0ms");
        } else {
            stats.put("average_response_time", String.format("%dms", totalResponseTimeMs / totalRequests));
        }
        stats.put("total_served_requests", String.format("%d", totalRequests));
        stats.put("total_serve_time", String.format("%dms", totalResponseTimeMs));

        String json = new Gson().toJson(stats);
        log(req, startTime);
        return json;
    }
}
