package main.java.com.wallet;

import main.java.com.wallet.repository.WalletRepository;
import main.java.com.wallet.service.OfferService;
import main.java.com.wallet.service.WalletService;

public class ApplicationContext {
    private final WalletService walletService;
    private final OfferService offerService;

    public ApplicationContext() {
        WalletRepository repository = new WalletRepository();

        this.walletService = new WalletService(repository);

        this.offerService = new OfferService(walletService);
    }

    public WalletService getWalletService() {
        return walletService;
    }

    public OfferService getOfferService() {
        return offerService;
    }
}