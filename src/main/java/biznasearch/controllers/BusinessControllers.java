package biznasearch.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.google.gson.Gson;

import org.apache.lucene.queryparser.classic.ParseException;

import biznasearch.models.Business;
import biznasearch.search_engine.LuceneWrapper;

public class BusinessControllers {

    public static String businessSearch(String query, int page, LuceneWrapper luc, int maxResults, String orderBy)
            throws IOException, ParseException, SQLException {
        long start = System.currentTimeMillis();
        List<Business> businesses = luc.search(query, page, maxResults, orderBy);
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(">>> " + query + ": " + elapsedTimeMillis + "ms for " + businesses.size() + " results");

        return new Gson().toJson(businesses);
    }

    public static String businessNameSimilars(String query, LuceneWrapper luc, int maxResults)
            throws IOException, ParseException, SQLException {
        long start = System.currentTimeMillis();
        List<String> similars = luc.getBusinessNameSuggestions(query, maxResults);
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(">>> " + query + ": " + elapsedTimeMillis + "ms for " + similars.size() + " results");
        return new Gson().toJson(similars);
    }
}
