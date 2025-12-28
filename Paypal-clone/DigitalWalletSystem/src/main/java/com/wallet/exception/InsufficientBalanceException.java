package main.java.com.wallet.exception;

public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException(String user) {
        super("Insufficient balance for user: " + user);
    }
}