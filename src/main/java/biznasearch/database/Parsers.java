package biznasearch.database;

import biznasearch.models.Business;
import biznasearch.models.Review;
import biznasearch.models.Tip;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

public class Parsers {
    public static Business parseBusiness(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Business(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getDouble(3),
                    rs.getDouble(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getString(10)
            );
        }

        return null;
    }

    public static Review parseReview(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Review(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getInt(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getInt(8)
            );
        }

        return null;
    }

    public static Tip parseTip(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return new Tip(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getString(5)
            );
        }

        return null;
    }

    static String sqlBusinessesByIDs(List<String> ids) {
        if (ids == null || ids.size() == 0) {
            return "SELECT * FROM businesses";
        }

        String sql = "SELECT * FROM businesses WHERE id in (";

        StringJoiner joiner = new StringJoiner(",");
        for (String id : ids) {
            joiner.add("'" + id + "'");
        }

        return sql + joiner.toString() + ")";
    }

}
