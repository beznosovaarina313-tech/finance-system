package model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class BackupData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Transaction> transactions;
    private final List<Category> categories;

    public BackupData(List<Transaction> transactions,
                           List<Category> categories,
                           Date backupDate) {
        this.transactions = transactions;
        this.categories = categories;

    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

}
