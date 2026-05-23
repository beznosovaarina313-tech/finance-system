package database;

import model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.io.*;

public class DatabaseManager {
    private Connection conn;

    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/...",
                    "...",
                    "..."
            );
            createTables();
        } catch (Exception e) {
            System.out.println("Ошибка подключения к БД: " + e.getMessage());
        }
    }

    public List<Transaction> loadTransactions() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT t.id, t.type, t.amount, t.date, t.description, "
                +
                "c.id as cat_id, c.name as cat_name, c.type as cat_type " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.id " +
                "ORDER BY t.date";

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            int id = rs.getInt("id");
            String type = rs.getString("type");
            double amount = rs.getDouble("amount");

            java.sql.Timestamp timestamp = rs.getTimestamp("date");
            Date date = new Date(timestamp.getTime());

            String description = rs.getString("description");

            Category category = new Category(
                    rs.getInt("cat_id"),
                    rs.getString("cat_name"),
                    rs.getString("cat_type")
            );

            Transaction transaction;
            if (type.equals("INCOME")) {
                transaction = new Income(amount, date, description, category);
            } else {
                transaction = new Expense(amount, date, description, category);
            }
            transaction.setId(id);

            transactions.add(transaction);
        }

        stmt.close();
        return transactions;
    }

    public double calculate() throws SQLException {
        double balance = 0;

        String sql = "SELECT " +
                "SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) - " +
                "SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) as balance " +
                "FROM transactions";

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            balance = rs.getDouble("balance");
            if (rs.wasNull()) {
                balance = 0;
            }
        }

        stmt.close();
        return balance;
    }

    public boolean categoryExists(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        boolean exists = rs.getInt(1) > 0;
        stmt.close();
        return exists;
    }

    public List<Category> getAllCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            categories.add(new Category(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("type")
            ));
        }
        stmt.close();
        return categories;
    }

    private void createTables() throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(100) UNIQUE, " +
                "type VARCHAR(10))");

        stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                "id SERIAL PRIMARY KEY, " +
                "type VARCHAR(10), " +
                "amount DECIMAL(10,2), " +
                "date TIMESTAMP, " +
                "description TEXT, " +
                "category_id INTEGER)");

        stmt.close();
    }

    public void saveCategory(Category cat) throws SQLException {
        String sql = "INSERT INTO categories (name, type) VALUES (?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, cat.getName());
        stmt.setString(2, cat.getType());
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            cat.setId(rs.getInt(1));
        }
        stmt.close();
    }

    public void saveTransaction(Transaction t) throws SQLException {
        String type = t instanceof Income ? "INCOME" : "EXPENSE";
        String sql = "INSERT INTO transactions (type, amount, date, description, category_id) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, type);
        stmt.setDouble(2, t.getAmount());

        java.sql.Timestamp timestamp = new java.sql.Timestamp(t.getDate().getTime());
        stmt.setTimestamp(3, timestamp);

        stmt.setString(4, t.getDescription());
        stmt.setInt(5, t.getCategory().getId());
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            t.setId(rs.getInt(1));
        }
        stmt.close();
    }

    public void createBackup(String filename) throws IOException, SQLException {
        List<Transaction> transactions = loadTransactions();
        List<Category> categories = getAllCategories();

        BackupData backupData = new BackupData(transactions, categories, new Date());

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(backupData);
            System.out.println("Копия создана: " + filename);
            System.out.println("Сохранено: " + transactions.size() +
                    " транзакций, " + categories.size() + " категорий");
        }
    }

    public void addFromBackup(String filename) throws IOException,
            ClassNotFoundException {

        BackupData backupData;
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            backupData = (BackupData) ois.readObject();
        }

        int addedTransactions = 0;
        for (Transaction t : backupData.getTransactions()) {
            try {
                Category existingCat = findCategory(
                        t.getCategory().getName(),
                        t.getCategory().getType()
                );

                if (existingCat != null) {
                    Transaction newTransaction;
                    if (t instanceof Income) {
                        newTransaction = new Income(
                                t.getAmount(), t.getDate(), t.getDescription(), existingCat
                        );
                    } else {
                        newTransaction = new Expense(
                                t.getAmount(), t.getDate(), t.getDescription(), existingCat
                        );
                    }
                    saveTransaction(newTransaction);
                    addedTransactions++;
                }
            } catch (SQLException e) {
                System.out.println("Ошибка добавления транзакции: " + e.getMessage());
            }
        }

        System.out.println("Добавлено транзакций: " + addedTransactions);
    }

    private Category findCategory(String name, String type) throws SQLException {
        String sql = "SELECT id, name, type FROM categories WHERE name = ? AND type = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, name);
        stmt.setString(2, type);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new Category(rs.getInt("id"), rs.getString("name"), rs.getString("type"));
        }
        stmt.close();
        return null;
    }
}