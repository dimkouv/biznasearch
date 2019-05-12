package biznasearch.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

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

    public String getSearchResults(Request req, Response res) throws SQLException,
            org.apache.lucene.queryparser.classic.ParseException, IOException, InvalidTokenOffsetsException {
        asJSON(res);

        if (missingParamsExist(req, new String[] { "query", "order-by", "results-num" })) {
            res.status(400);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            return jsonMessage("Query is empty");
        }

        List<String> acceptedOrderCols = Arrays.asList(
            "review_count", "-review_count", "stars", "-stars", "clicks","-clicks", "");

        if (!acceptedOrderCols.contains(req.queryParams("order-by"))) {
            res.status(404);
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

        long start = System.currentTimeMillis();
        List<Business> businesses = luc.search(req.queryParams("query"), resultsNum, req.queryParams("order-by"));
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(
            ">>> " + req.queryParams("query") + ": " + elapsedTimeMillis + "ms for " + businesses.size() + " results");

        return new Gson().toJson(businesses);
    }

    public String getQuerySpellSuggestions(Request req, Response res) throws IOException {
        asJSON(res);

        if (missingParamsExist(req, new String[] { "query" })) {
            res.status(400);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            return jsonMessage("Query is empty");
        }

        long start = System.currentTimeMillis();
        List<String> similars = luc.getBusinessNameSuggestions(req.queryParams("query"), 10);
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(
            ">>> " + req.queryParams("query") + ": " + elapsedTimeMillis + "ms for " + similars.size() + " results");
        return new Gson().toJson(similars);
    }

    public String startIndexing(Request req, Response res) {
        asJSON(res);

        if (missingParamsExist(req, new String[] { "token" })) {
            res.status(400);
            return jsonMessage("Some required parameters are missing");
        }

        if (!req.queryParams("token").equals(authToken)) {
            res.status(400);
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

        return jsonMessage("Indexing operation started");
    }

    public String logClick(Request req, Response res) {
        asJSON(res);

        if (missingParamsExist(req, new String[] { "business-id" })) {
            res.status(400);
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

        return jsonMessage("logged");
    }

    public String getQuerySuggestions(Request req, Response res) throws SQLException {
        asJSON(res);
        if (missingParamsExist(req, new String[] { "query" })) {
            res.status(400);
            return jsonMessage("Some required parameters are missing");
        }

        if (req.queryParams("query").length() == 0) {
            res.status(400);
            return jsonMessage("Query is empty");
        }

        long start = System.currentTimeMillis();
        List<Query> similars = Getters.similarQueries(luc.getDBConnection(), req.queryParams("query"), 10);
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(
            ">>> " + req.queryParams("query") + ": " + elapsedTimeMillis + "ms for " + similars.size() + " suggestions");
        return new Gson().toJson(similars);
    }
}
