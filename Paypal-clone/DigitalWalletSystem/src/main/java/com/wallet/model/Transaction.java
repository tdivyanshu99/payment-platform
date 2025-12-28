package main.java.com.wallet.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private final String counterPartyId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;

    public Transaction(String counterPartyId, TransactionType type, BigDecimal amount) {
        this.counterPartyId = counterPartyId;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    @Override
    public String toString() {
        return counterPartyId + " " + type.toString().toLowerCase() + " " + amount.stripTrailingZeros().toPlainString();
    }
}