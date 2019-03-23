import database.ConnectionManager;
import database.models.Business;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static database.Utilities.nextBusiness;

public class Server {
    private ConnectionManager cm;

    public Server() throws SQLException {
        cm = new ConnectionManager(
                "jdbc:postgresql://192.168.0.10:5432/biznasearch",
                "sysdba",
                "masterkey"
        );
    }

    public void stop() throws SQLException {
        cm.closeConnection();
    }

    public void indexBusinesses() throws SQLException {
        /** loop through businesses */
        Business business;
        String query = "SELECT * FROM businesses";

        PreparedStatement pst = cm.getConnection().prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        business = nextBusiness(rs);
        while(business != null) {
            System.out.println(business.getLat());
            business = nextBusiness(rs);
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.indexBusinesses();
            server.stop();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
