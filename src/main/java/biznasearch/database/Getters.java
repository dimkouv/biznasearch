package biznasearch.database;

import biznasearch.models.Business;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static biznasearch.database.Parsers.*;

public class Getters {
    public static List<Business> businessesByIDs(Connection con, List<String> ids) throws SQLException {
        /* Returns a list of businesses by their IDs */

        String sql = sqlBusinessesByIDs(ids);

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

}
