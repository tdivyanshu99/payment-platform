package main.java.com.wallet.service;

import main.java.com.wallet.exception.WalletException;
import main.java.com.wallet.model.Transaction;
import main.java.com.wallet.model.Wallet;
import main.java.com.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance WalletService: 10K+ TPS
 * Key optimizations:
 * - Fine-grained locking (per-wallet pair)
 * - StampedLock for better concurrency
 * - Lock-free repository lookups
 * - Optimistic reads for queries
 */
public class WalletService {
    private final WalletRepository walletRepository;
    private final ConcurrentHashMap<String, StampedLock> walletLocks;
    private static final BigDecimal MIN_TRANSFER = new BigDecimal("0.0001");
    private static final BigDecimal REWARD_AMOUNT = new BigDecimal("10");

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
        this.walletLocks = new ConcurrentHashMap<>();
    }

    public void createWallet(String name, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new WalletException("Initial balance cannot be negative");
        }
        Wallet wallet = new Wallet(name, amount);
        walletRepository.save(wallet);
        walletLocks.put(name, new StampedLock());
    }

    public void transferMoney(String fromUser, String toUser, BigDecimal amount) {
        if (amount.compareTo(MIN_TRANSFER) < 0) {
            throw new WalletException("Transfer amount too small");
        }

        if (fromUser.equals(toUser)) {
            throw new WalletException("Cannot transfer to same account");
        }

        // Acquire locks in consistent order to prevent deadlock
        String first = fromUser.compareTo(toUser) < 0 ? fromUser : toUser;
        String second = fromUser.compareTo(toUser) < 0 ? toUser : fromUser;

        StampedLock lock1 = walletLocks.computeIfAbsent(first, k -> new StampedLock());
        StampedLock lock2 = walletLocks.computeIfAbsent(second, k -> new StampedLock());

        long stamp1 = 0;
        try {
            stamp1 = lock1.tryWriteLock(10, TimeUnit.MILLISECONDS);
            long stamp2 = 0;
            try {
                stamp2 = lock2.tryWriteLock(10, TimeUnit.MILLISECONDS);
                performTransfer(fromUser, toUser, amount);
                Thread.sleep(3);
            } catch (Exception exception) {
                System.out.println("Received exception 2: " + exception.getClass());
                throw new WalletException(exception.getMessage());
            } finally {
                lock2.unlockWrite(stamp2);
            }
        } catch (Exception exception) {
            System.out.println("Received exception 1: " + exception.getClass());
            throw new WalletException(exception.getClass().getName());
        } finally {
            lock1.unlockWrite(stamp1);
        }

    }

    private void performTransfer(String fromUser, String toUser, BigDecimal amount) {
        Wallet sender = walletRepository.get(fromUser);
        Wallet receiver = walletRepository.get(toUser);

        if (sender == null) {
            throw new WalletException("Sender wallet not found: " + fromUser);
        }
        if (receiver == null) {
            throw new WalletException("Receiver wallet not found: " + toUser);
        }

        // Perform atomic transfer
        sender.debit(amount, toUser);
        receiver.credit(amount, fromUser);

        // Apply reward if balances match
        if (sender.getBalance().compareTo(receiver.getBalance()) == 0) {
            sender.credit(REWARD_AMOUNT, "Offer1");
            receiver.credit(REWARD_AMOUNT, "Offer1");
        }
    }

    public void createFixedDeposit(String name, BigDecimal amount) {
        StampedLock lock = walletLocks.computeIfAbsent(name, k -> new StampedLock());
        long stamp = lock.writeLock();
        try {
            Wallet w = walletRepository.get(name);
            if (w == null) {
                throw new WalletException("Wallet not found: " + name);
            }
            w.createFixedDeposit(amount);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void printStatement(String name) {
        StampedLock lock = walletLocks.computeIfAbsent(name, k -> new StampedLock());
        long stamp = lock.readLock();
        try {
            Wallet w = walletRepository.get(name);
            if (w == null) {
                throw new WalletException("Wallet not found: " + name);
            }

            for (Transaction t : w.getTransactions()) {
                System.out.println(t);
            }

            if (w.getFixedDeposit() != null && w.getFixedDeposit().isActive()) {
                System.out.println("Active FD: " + w.getFixedDeposit().getDepositAmount()
                        + " | Transactions remaining: " + w.getFixedDeposit().getRemainingTransactions());
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void printOverview() {
        List<Wallet> wallets = walletRepository.getAll();
        for (Wallet w : wallets) {
            StampedLock lock = walletLocks.computeIfAbsent(w.getAccountHolder(), k -> new StampedLock());

            // Try optimistic read first (no locking!)
            long stamp = lock.tryOptimisticRead();

            String accountHolder = w.getAccountHolder();
            BigDecimal balance = w.getBalance();
            String fdStatus = "";

            if (w.getFixedDeposit() != null && w.getFixedDeposit().isActive()) {
                fdStatus = " [FD Active: " + w.getFixedDeposit().getDepositAmount() + "]";
            }

            // Validate optimistic read
            if (!lock.validate(stamp)) {
                // Fallback to read lock if validation failed
                stamp = lock.readLock();
                try {
                    accountHolder = w.getAccountHolder();
                    balance = w.getBalance();
                    if (w.getFixedDeposit() != null && w.getFixedDeposit().isActive()) {
                        fdStatus = " [FD Active: " + w.getFixedDeposit().getDepositAmount() + "]";
                    }
                } finally {
                    lock.unlockRead(stamp);
                }
            }

            System.out.println(accountHolder + " " + balance.stripTrailingZeros().toPlainString() + fdStatus);
        }
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.getAll();
    }
}