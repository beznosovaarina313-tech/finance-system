package model;

import database.DatabaseManager;
import exceptions.InsufficientFundsException;
import exceptions.DuplicateCategoryException;

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public class Wallet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient DatabaseManager db;
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();

    public Wallet() {
        db = new DatabaseManager();
        loadAllDataFromDB();
    }

    private void loadAllDataFromDB() {
        try {
            List<Category> dbCategories = db.getAllCategories();
            categories.addAll(dbCategories);

            List<Transaction> dbTransactions = db.loadTransactions();
            transactions.addAll(dbTransactions);

        } catch (SQLException e) {
            System.out.println("Ошибка загрузки данных: " + e.getMessage());
        }
    }

    public List<Category> getCategoriesFromDB() throws SQLException {
        return db.getAllCategories();
    }

    public void addCategory(Category cat) throws DuplicateCategoryException {
        try {
            if (db.categoryExists(cat.getName())) {
                throw new DuplicateCategoryException("Категория уже существует: " + cat.getName());
            }

            for (Category c : categories) {
                if (c.getName().equals(cat.getName())) {
                    throw new DuplicateCategoryException("Категория уже существует: " + cat.getName());
                }
            }

            db.saveCategory(cat);
            categories.add(cat);

        } catch (SQLException e) {
            throw new DuplicateCategoryException("Ошибка подключения к БД: " + e.getMessage());
        }
    }

    public void showBalance() {
        try {
            double balance = db.calculate();
            System.out.println("Текущий баланс: " + balance);
        } catch (SQLException e) {
            System.out.println("Ошибка получения баланса: " + e.getMessage());
        }
    }

    public void printMonthlyStatistics(int month, int year) {
        double totalIncome = 0;
        double totalExpense = 0;

        Calendar cal = Calendar.getInstance();

        for (Transaction t : transactions) {
            cal.setTime(t.getDate());
            int tMonth = cal.get(Calendar.MONTH) + 1;
            int tYear = cal.get(Calendar.YEAR);

            if (tMonth == month && tYear == year) {
                if (t instanceof Income) {
                    totalIncome += t.getAmount();
                } else if (t instanceof Expense) {
                    totalExpense += t.getAmount();
                }
            }
        }

        System.out.println("\nСтатистика за " + month + "." + year + " ");
        System.out.println("Доходы: " + totalIncome);
        System.out.println("Расходы: " + totalExpense);
    }

    public void addTransaction(Transaction t) throws InsufficientFundsException {
        if (t instanceof Expense) {
            try {
                double currentBalance = db.calculate();
                if (currentBalance < t.getAmount()) {
                    throw new InsufficientFundsException("Недостаточно средств. Баланс: " + currentBalance);
                }
            } catch (SQLException e) {
                throw new InsufficientFundsException("Ошибка проверки баланса: " + e.getMessage());
            }
        }

        try {
            db.saveTransaction(t);
        } catch (SQLException e) {
            System.out.println("Не сохранилось в БД: " + e.getMessage());
            throw new InsufficientFundsException("Ошибка сохранения транзакции");
        }

        transactions.add(t);
        System.out.println("Добавлено: " + t);
    }


    public void createBackup(String filename) {
        try {
            db.createBackup(filename);
        } catch (Exception e) {
            System.out.println("Ошибка копирования: " + e.getMessage());
        }
    }

    public void addFromBackup(String filename) {
        try {
            db.addFromBackup(filename);

            transactions.clear();
            categories.clear();
            loadAllDataFromDB();
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}
