package model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public abstract class Transaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected int id;
    protected double amount;
    protected Date date;
    protected String description;
    protected Category category;

    public Transaction(double amount, Date date, String description, Category category) {
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.category = category;
    }

    public abstract String getDisplayType();

    public double getAmount() {
        return amount;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getDisplayType() + "-" + amount + " (" + description + ")";
    }
}

