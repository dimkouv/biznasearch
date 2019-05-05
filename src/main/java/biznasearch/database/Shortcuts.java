package biznasearch.database;

import java.util.List;
import java.util.StringJoiner;

public class Shortcuts {
    public static String sqlBusinessesByIDs(List<String> ids, String orderBy) {
        if (ids == null || ids.size() == 0) {
            return "SELECT * FROM businesses WHERE 0 = 1"; // a query that returns nothing
        }

        String sql = "SELECT * FROM businesses WHERE id in (";
        StringJoiner joiner = new StringJoiner(",");
        for (String id : ids) {
            joiner.add("'" + id + "'");
        }
        sql = sql + joiner.toString() + ")";

        if (orderBy == null || orderBy.equals("")) {
            return sql;
        } else {
            if (orderBy.startsWith("-")) {
                sql += " ORDER BY " + orderBy.substring(1) + " DESC";
            } else {
                sql += " ORDER BY " + orderBy + " ASC";
            }
        }

        return sql;
    }

    public static String sqlReviewsIdxColsWhereCityIs(String city) {
        String sql = "SELECT r.business_id, r.text FROM reviews r INNER JOIN businesses b on r.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s' LIMIT 1250000", city);

        return sql;
    }

    public static String sqlTipsIdxColsWhereCityIs(String city) {
        String sql = "SELECT t.business_id, t.text FROM tips t INNER JOIN businesses b on t.business_id = b.id";
        sql += String.format(" WHERE b.city = '%s'", city);

        return sql;
    }

    public static String sqlBusinessesIdxColsOfCity(String city) {
        return String.format("SELECT id, name, categories FROM businesses WHERE city = '%s'", city);
    }
}
