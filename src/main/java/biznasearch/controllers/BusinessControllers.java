package biznasearch.controllers;

import biznasearch.models.Business;
import biznasearch.search_engine.LuceneWrapper;
import com.google.gson.Gson;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class BusinessControllers {

    public static String businessSearch(String query, int page, LuceneWrapper luc, int maxResults) throws IOException, ParseException, SQLException {
        long start = System.currentTimeMillis();
        List<Business> businesses = luc.searchBusinesses(query, page, maxResults);
        long elapsedTimeMillis = System.currentTimeMillis() - start;
        System.out.println(query + ": " + elapsedTimeMillis + "ms for " + businesses.size() + " results");

        return new Gson().toJson(businesses);
    }
}
