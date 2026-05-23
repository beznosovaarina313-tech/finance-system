import java.sql.*;

public class CheckSQL {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/...";
        String user = "...";
        String password = "...";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Успешное подключение");

            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id INT, name VARCHAR(50))");
            System.out.println("Можем создавать таблицы");

            stmt.execute("INSERT INTO test_table VALUES (1, 'Test')");
            System.out.println("Можем вставлять данные");

            stmt.execute("DROP TABLE test_table");
            System.out.println("Можем удалять таблицы");

            conn.close();
            System.out.println("\nПользователь имеет все необходимые права");

        } catch (SQLException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}