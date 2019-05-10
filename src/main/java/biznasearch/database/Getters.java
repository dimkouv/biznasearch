package biznasearch.database;

import biznasearch.models.Business;
import biznasearch.models.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static biznasearch.database.Parsers.parseBusiness;
import static biznasearch.database.Parsers.parseQuery;
import static biznasearch.database.Shortcuts.sqlBusinessesByIDs;

public class Getters {
    public static List<Business> businessesByIDs(Connection con, List<String> ids, String orderBy) throws SQLException {
        /* Returns a list of businesses by their IDs */

        String sql = sqlBusinessesByIDs(ids, orderBy);
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        List<Business> result = new ArrayList<>();

        Business business = parseBusiness(rs);
        while (business != null) {
            result.add(business);
            business = parseBusiness(rs);
        }

        System.out.println(sql + " results:" + result.size());

        return result;
    }

    public static List<Query> similarQueries(Connection con, String query, int suggestionsNum) throws SQLException {
        /* Returns a list of similar queries */

        String sql = Shortcuts.sqlSimilarQueries(query, suggestionsNum);
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        List<Query> result = new ArrayList<>();

        Query q = parseQuery(rs);
        while (q != null) {
            result.add(q);
            q = parseQuery(rs);
        }

        return result;
    }
}
