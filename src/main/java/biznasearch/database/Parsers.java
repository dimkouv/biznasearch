package biznasearch.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import biznasearch.models.Business;
import biznasearch.models.Query;
import biznasearch.models.Review;
import biznasearch.models.Tip;

public class Parsers {
    public static Business parseBusiness(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Business(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4), rs.getString(5),
                    rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getInt(11));
        }

        return null;
    }

    public static Review parseReview(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Review(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5),
                    rs.getInt(6), rs.getInt(7), rs.getInt(8));
        }

        return null;
    }

    public static Tip parseTip(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Tip(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5));
        }

        return null;
    }

    public static Query parseQuery(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Query(rs.getLong(1), rs.getString(2), rs.getInt(3));
        }

        return null;
    }
}
