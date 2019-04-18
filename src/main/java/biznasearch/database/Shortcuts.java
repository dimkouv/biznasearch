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

    public static String sqlReviewsIdxColsWhereCityIs(String city) {
        String sql = "SELECT r.business_id, r.text FROM reviews r INNER JOIN businesses b on r.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s' LIMIT 1000000", city);

        return sql;
    }

    public static String sqlTipsIdxColsWhereCityIs(String city) {
        String sql = "SELECT t.business_id, t.text FROM tips t INNER JOIN businesses b on t.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s'", city);

        return sql;
    }

    public static String sqlBusinessesIdxColsOfCity(String city) {
        return String.format("SELECT id, name FROM businesses WHERE city = '%s'", city);
    }
}
