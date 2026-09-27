import java.sql.*;

public class InspectMembers {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://ep-cold-mud-a95ocgs5-pooler.gwc.azure.neon.tech/neondb?sslmode=require",
                "neondb_owner", "npg_eUHFa4Z7AMuC")) {
            ResultSet rs = c.createStatement().executeQuery(
                    "SELECT column_name, data_type, character_maximum_length FROM information_schema.columns WHERE table_name='members' ORDER BY ordinal_position");
            while (rs.next()) {
                System.out.printf("%s %s %s%n", rs.getString(1), rs.getString(2), rs.getString(3));
            }
        }
    }
}
