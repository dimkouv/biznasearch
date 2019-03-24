package database;

import database.models.Business;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Utilities {
    public static Business nextBusiness(ResultSet rs) throws SQLException {
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
                    rs.getString(9)
            );
        }

        return null;
    }
}
