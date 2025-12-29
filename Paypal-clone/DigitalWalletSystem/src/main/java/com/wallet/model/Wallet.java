package main.java.com.wallet.model;

import main.java.com.wallet.exception.InsufficientBalanceException;
import main.java.com.wallet.exception.WalletException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe Wallet model with atomic operations
 */
public class Wallet {
    private final String accountHolder;
    private final AtomicReference<BigDecimal> balance;
    private final LocalDateTime createdAt;
    private final CopyOnWriteArrayList<Transaction> transactions;
    private final AtomicReference<FixedDeposit> fixedDeposit;

    public Wallet(String accountHolder, BigDecimal openingBalance) {
        this.accountHolder = accountHolder;
        this.balance = new AtomicReference<>(openingBalance);
        this.createdAt = LocalDateTime.now();
        this.transactions = new CopyOnWriteArrayList<>();
        this.fixedDeposit = new AtomicReference<>(null);
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public FixedDeposit getFixedDeposit() {
        return fixedDeposit.get();
    }

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

    public synchronized void credit(BigDecimal amount, String from) {
        BigDecimal newBalance = balance.get().add(amount);
        balance.set(newBalance);
        transactions.add(new Transaction(from, TransactionType.CREDIT, amount));
        checkFixedDepositStatus();
    }

    public synchronized void debit(BigDecimal amount, String to) {
        BigDecimal currentBalance = balance.get();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountHolder);
        }
        BigDecimal newBalance = currentBalance.subtract(amount);
        balance.set(newBalance);
        transactions.add(new Transaction(to, TransactionType.DEBIT, amount));
        checkFixedDepositStatus();
    }

    public synchronized void createFixedDeposit(BigDecimal amount) {
        BigDecimal currentBalance = balance.get();
        if (currentBalance.compareTo(amount) < 0) {
            throw new WalletException("Balance must be greater than FD amount.");
        }
        fixedDeposit.set(new FixedDeposit(amount));
    }

    private void checkFixedDepositStatus() {
        FixedDeposit fd = fixedDeposit.get();
        if (fd == null || !fd.isActive()) return;

        if (balance.get().compareTo(fd.getDepositAmount()) < 0) {
            fd.dissolve();
            return;
        }

        boolean matured = fd.decrementAndCheckMaturity();
        if (matured) {
            BigDecimal newBalance = balance.get().add(new BigDecimal("10"));
            balance.set(newBalance);
            transactions.add(new Transaction("FD_Interest", TransactionType.CREDIT, new BigDecimal("10")));
            fd.dissolve();
        }
    }
}