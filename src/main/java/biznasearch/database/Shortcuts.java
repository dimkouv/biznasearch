package biznasearch.database;

import java.util.List;
import java.util.StringJoiner;

public class Shortcuts {
    public static String sqlBusinessesByIDs(List<String> ids) {
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

    public static String sqlReviewsWhereCityIs(String city) {
        String sql = "SELECT r.* FROM reviews r INNER JOIN businesses b on r.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s'", city);

        return sql;
    }

    public static String sqlTipsWhereCityIs(String city) {
        String sql = "SELECT t.* FROM tips t INNER JOIN businesses b on t.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s'", city);

        return sql;
    }

    public static String sqlBusinessesOfCity(String city) {
        return String.format("SELECT * FROM businesses WHERE city = '%s'", city);
    }
}
