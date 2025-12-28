package main.java.com.wallet.model;

import main.java.com.wallet.exception.InsufficientBalanceException;
import main.java.com.wallet.exception.WalletException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Wallet {
    private final String accountHolder;
    private BigDecimal balance;
    private final LocalDateTime createdAt;
    private final List<Transaction> transactions;
    private FixedDeposit fixedDeposit;

    public Wallet(String accountHolder, BigDecimal openingBalance) {
        this.accountHolder = accountHolder;
        this.balance = openingBalance;
        this.createdAt = LocalDateTime.now();
        this.transactions = new ArrayList<>();
        this.fixedDeposit = null;
    }

    public String getAccountHolder() { return accountHolder; }
    public BigDecimal getBalance() { return balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Transaction> getTransactions() { return transactions; }
    public FixedDeposit getFixedDeposit() { return fixedDeposit; }

    public int getUserTransactionCount() {
        int count = 0;
        for (Transaction t : transactions) {
            if (!t.getCounterPartyId().equals("Offer1") &&
                    !t.getCounterPartyId().equals("Offer2") &&
                    !t.getCounterPartyId().equals("FD_Interest")) {
                count++;
            }
        }
        return count;
    }

    public void credit(BigDecimal amount, String from) {
        this.balance = this.balance.add(amount);
        this.transactions.add(new Transaction(from, TransactionType.CREDIT, amount));
        checkFixedDepositStatus();
    }

    public void debit(BigDecimal amount, String to) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountHolder);
        }
        this.balance = this.balance.subtract(amount);
        this.transactions.add(new Transaction(to, TransactionType.DEBIT, amount));
        checkFixedDepositStatus();
    }

    public void createFixedDeposit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new WalletException("Balance must be greater than FD amount.");
        }
        this.fixedDeposit = new FixedDeposit(amount);
    }

    private void checkFixedDepositStatus() {
        if (fixedDeposit == null || !fixedDeposit.isActive()) return;

        if (this.balance.compareTo(fixedDeposit.getDepositAmount()) < 0) {
            fixedDeposit.dissolve();
            return;
        }

        boolean matured = fixedDeposit.decrementAndCheckMaturity();
        if (matured) {
            credit(new BigDecimal("10"), "FD_Interest");
            fixedDeposit.dissolve();
        }
    }
}