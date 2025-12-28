package main.java.com.wallet.repository;

import main.java.com.wallet.exception.DuplicateWalletException;
import main.java.com.wallet.exception.WalletException;
import main.java.com.wallet.model.Wallet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hashmap based in-memory storage to store the wallet corresponding to each user.
 */
public class WalletRepository {
    private final Map<String, Wallet> walletMap = new ConcurrentHashMap<>();
    static WalletRepository instance = null;

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


    private WalletRepository() {

    }

    public static WalletRepository getInstance() {
        synchronized (instance) {
            if(instance == null)
                instance = new WalletRepository();
        }
        return instance;
    }



}

//