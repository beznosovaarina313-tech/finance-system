package model;
import java.util.Date;

public class Expense extends Transaction {

    public Expense(double amount, Date date, String description, Category category) {
        super(amount, date, description, category);
    }

    @Override
    public String getDisplayType() {
        return "Расход";
    }
}

