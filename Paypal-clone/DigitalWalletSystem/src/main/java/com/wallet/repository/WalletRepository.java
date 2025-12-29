package main.java.com.wallet.repository;

import main.java.com.wallet.exception.DuplicateWalletException;
import main.java.com.wallet.exception.WalletException;
import main.java.com.wallet.model.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lock-free, thread-safe wallet repository
 * Optimized for 10K+ concurrent operations
 */
public class WalletRepository {
    private final ConcurrentHashMap<String, Wallet> walletMap;

    public WalletRepository() {
        // Initial capacity: 16K wallets, 16 concurrent segments
        this.walletMap = new ConcurrentHashMap<>(16384, 0.75f, 16);
    }

    public void save(Wallet wallet) {
        if (walletMap.containsKey(wallet.getAccountHolder())) {
            throw new DuplicateWalletException(wallet.getAccountHolder());
        }
        walletMap.put(wallet.getAccountHolder(), wallet);
    }

    public Wallet get(String accountHolder) {
        Wallet w = walletMap.getOrDefault(accountHolder, null);
        if (w == null) {
            throw new WalletException("Wallet not found: " + accountHolder);
        }
        return w;
    }

    public List<Wallet> getAll() {
        return new ArrayList<>(walletMap.values());
    }
}