package database;

import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {
    java.sql.Connection con;

    public ConnectionManager(String url, String username, String password) throws SQLException {
        con = DriverManager.getConnection(url, username, password);
    }

    public java.sql.Connection getConnection() {
        return con;
    }

    public void closeConnection() throws SQLException {
        con.close();
    }
}
