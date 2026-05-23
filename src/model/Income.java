package model;
import java.util.Date;

public class Income extends Transaction {

    public Income(double amount, Date date, String description, Category category) {
        super(amount, date, description, category);
    }

    @Override
    public String getDisplayType() {
        return "Доход";
    }
}

