import model.*;
import exceptions.*;
import java.util.Date;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.util.InputMismatchException;
import java.io.File;

public class Main {
    static Wallet wallet;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("МОИ ФИНАНСЫ");
        wallet = new Wallet();

        while (true) {
            System.out.println("МЕНЮ");
            System.out.println("1. Добавить доход");
            System.out.println("2. Добавить расход");
            System.out.println("3. Баланс");
            System.out.println("4. Статистика");
            System.out.println("5. Создать копию");
            System.out.println("6. Восстановить данные");
            System.out.println("7. Выход");

            int choice = getInt("Выберите опцию: ");

            switch (choice) {
                case 1 -> addTransaction("INCOME");
                case 2 -> addTransaction("EXPENSE");
                case 3 -> wallet.showBalance();
                case 4 -> showStatistics();
                case 5 -> createBackup();
                case 6 -> restore();
                case 7 -> {
                    System.out.println("Выход");
                    System.exit(0);
                }
                default -> System.out.println("Выберите опцию");
            }
        }
    }

    static void addTransaction(String type) {
        System.out.print("Сумма: ");
        double amount;

        while (true) {
            try {
                amount = scanner.nextDouble();
                scanner.nextLine();
                break;
            } catch (InputMismatchException e) {
                System.out.println("Введите число.");
                scanner.nextLine();
                System.out.print("Сумма: ");
            }
        }

        System.out.print("Описание: ");
        String desc = scanner.nextLine();

        try {
            List<Category> allCategories = wallet.getCategoriesFromDB();
            List<Category> filteredCategories = new ArrayList<>();

            String typeName = type.equals("INCOME") ? "доходов" : "расходов";

            while (true) {
                System.out.println("\nВыберите категорию " + typeName + " ");

                int i = 1;
                for (Category cat : allCategories) {
                    if (cat.getType().equals(type)) {
                        System.out.println(i + ". " + cat.getName());
                        filteredCategories.add(cat);
                        i++;
                    }
                }

                System.out.println(i + ". Создать новую категорию");

                int catChoice = getInt("\nВаш выбор: ");
                scanner.nextLine();

                if (catChoice == i) {
                    Category newCat = createCategory(type);
                    if (newCat != null) {
                        addTransaction(amount, desc, newCat, type);
                    }
                    break;
                } else if (catChoice > 0 && catChoice < i) {
                    Category selectedCat = filteredCategories.get(catChoice - 1);
                    addTransaction(amount, desc, selectedCat, type);
                    break;
                } else {
                    System.out.println("Неверный выбор");
                    filteredCategories.clear();
                }
            }

        } catch (SQLException e) {
            System.out.println("Ошибка загрузки категорий: " + e.getMessage());
        }
    }

    static Category createCategory(String type) {
        String name;
        do {
            System.out.print("\nВведите название новой категории: ");
            name = scanner.nextLine().trim();
        } while (name.isEmpty());

        try {
            Category cat = new Category(0, name, type);
            wallet.addCategory(cat);
            System.out.println("Категория '" + name + "' создана");
            return cat;
        } catch (DuplicateCategoryException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return null;
        }
    }

    static void addTransaction(double amount, String desc,
                               Category category, String type) {
        try {
            Transaction transaction;
            if (type.equals("INCOME")) {
                transaction = new Income(amount, new Date(), desc, category);
            } else {
                transaction = new Expense(amount, new Date(), desc, category);
            }

            wallet.addTransaction(transaction);

        } catch (InsufficientFundsException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    static void showStatistics() {
        int month = 0;
        while (month < 1 || month > 12) {
            System.out.print("\nВведите месяц (1-12): ");
            month = getInt("");

            if (month < 1 || month > 12) {
                System.out.println("Неверный выбор");
            }
        }

        int year = 0;
        while (year < 2000 || year > 2100) {
            System.out.print("Введите год: ");
            year = getInt("");

            if (year < 2025 || year > 2100) {
                System.out.println("Год должен быть между 2025 и 2100.");
            }
        }

        scanner.nextLine();
        wallet.printMonthlyStatistics(month, year);
    }


    static void createBackup() {
        scanner.nextLine();

        System.out.print("\nВведите имя файла для копирования: ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            filename = "backup_" + System.currentTimeMillis() + ".ser";
            System.out.println("Используется имя по умолчанию: " + filename);
        }

        wallet.createBackup(filename);
    }

    static int getInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Неверный выбор");
                scanner.nextLine();
            }
        }
    }

    static void restore() {
        scanner.nextLine();

        System.out.println("\nВосстановление данных");

        System.out.print("Введите имя файла: ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            System.out.println("Имя файла не может быть пустым");
            return;
        }

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Файл не найден: " + filename);
            return;
        }
        wallet.addFromBackup(filename);
    }
}