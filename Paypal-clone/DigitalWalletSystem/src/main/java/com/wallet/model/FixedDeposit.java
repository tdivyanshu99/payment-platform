package main.java.com.wallet.model;

import java.math.BigDecimal;

public class FixedDeposit {
    private final BigDecimal depositAmount;
    private int remainingTransactionsToCheck;
    private boolean isActive;

    public FixedDeposit(BigDecimal amount) {
        this.depositAmount = amount;
        this.remainingTransactionsToCheck = 5;
        this.isActive = true;
    }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public boolean isActive() { return isActive; }
    public void dissolve() { this.isActive = false; }

    public boolean decrementAndCheckMaturity() {
        if (!isActive) return false;
        this.remainingTransactionsToCheck--;
        return this.remainingTransactionsToCheck == 0;
    }

    public int getRemainingTransactions() { return remainingTransactionsToCheck; }
}