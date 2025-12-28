package main.java.com.wallet.service;

import main.java.com.wallet.model.Wallet;
import java.math.BigDecimal;
import java.util.List;

/**
 * Handles the business logic for applying offers.
 * Specifically manages the complex sorting and rewarding for Offer 2.
 */
public class OfferService {
    private final WalletService walletService;

    public OfferService(WalletService walletService) {
        this.walletService = walletService;
    }

    public void triggerOffer2() {
        List<Wallet> allWallets = walletService.getAllWallets();

        allWallets.sort((w1, w2) -> {
            int txCompare = Integer.compare(w2.getUserTransactionCount(), w1.getUserTransactionCount());
            if (txCompare != 0) return txCompare;

            int balCompare = w2.getBalance().compareTo(w1.getBalance());
            if (balCompare != 0) return balCompare;

            return w1.getCreatedAt().compareTo(w2.getCreatedAt());
        });

        BigDecimal[] rewards = {new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2")};

        for (int i = 0; i < Math.min(allWallets.size(), 3); i++) {
            Wallet winner = allWallets.get(i);
            winner.credit(rewards[i], "Offer2");
        }
    }
}