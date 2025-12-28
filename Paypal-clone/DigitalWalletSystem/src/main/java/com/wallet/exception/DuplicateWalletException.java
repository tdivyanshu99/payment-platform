package main.java.com.wallet.exception;

public class DuplicateWalletException extends WalletException {
    public DuplicateWalletException(String user) {
        super("Wallet already exists for user: " + user);
    }
}