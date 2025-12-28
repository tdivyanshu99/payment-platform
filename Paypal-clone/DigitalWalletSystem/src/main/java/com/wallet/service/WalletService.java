package main.java.com.wallet.service;

import main.java.com.wallet.exception.WalletException;
import main.java.com.wallet.model.Transaction;
import main.java.com.wallet.model.Wallet;
import main.java.com.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public class for handling operations for managing transactions of users.
 * Transactions supported are: CreateWallet, TransferMoney, CreateFixedDeposit, PrintStatement, Overview
 */
public class WalletService {
    private final WalletRepository walletRepository;
    private static final BigDecimal MIN_TRANSFER = new BigDecimal("0.0001");

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public void createWallet(String name, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletException("Initial balance cannot be negative");
        }
        Wallet wallet = new Wallet(name, amount);
        walletRepository.save(wallet);
    }

    public void transferMoney(String fromUser, String toUser, BigDecimal amount) {

        Wallet sender = walletRepository.get(fromUser);
        Wallet receiver = walletRepository.get(toUser);

        final Object lock1;
        final Object lock2;

        if (fromUser.compareTo(toUser) < 0) {
            lock1 = sender;
            lock2 = receiver;
        } else {
            lock1 = receiver;
            lock2 = sender;
        }

        synchronized (lock1) {
            synchronized (lock2) {
                sender.debit(amount, toUser);
                receiver.credit(amount, fromUser);

                if (sender.getBalance().compareTo(receiver.getBalance()) == 0) {
                    BigDecimal reward = new BigDecimal("10");
                    sender.credit(reward, "Offer1");
                    receiver.credit(reward, "Offer1");
                }
            }
        }
    }

    public void createFixedDeposit(String name, BigDecimal amount) {
        Wallet w = walletRepository.get(name);
        w.createFixedDeposit(amount);
    }

    public void printStatement(String name) {
        Wallet w = walletRepository.get(name);
        for (Transaction t : w.getTransactions()) {
            System.out.println(t);
        }
        if (w.getFixedDeposit() != null && w.getFixedDeposit().isActive()) {
            System.out.println("Active FD: " + w.getFixedDeposit().getDepositAmount()
                    + " | Transactions remaining: " + w.getFixedDeposit().getRemainingTransactions());
        }
    }

    public void printOverview() {
        for (Wallet w : walletRepository.getAll()) {
            String fdStatus = "";
            if (w.getFixedDeposit() != null && w.getFixedDeposit().isActive()) {
                fdStatus = " [FD Active: " + w.getFixedDeposit().getDepositAmount() + "]";
            }
            System.out.println(w.getAccountHolder() + " "
                    + w.getBalance().stripTrailingZeros().toPlainString() + fdStatus);
        }
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.getAll();
    }
}