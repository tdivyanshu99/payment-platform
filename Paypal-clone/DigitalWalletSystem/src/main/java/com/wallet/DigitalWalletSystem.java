package main.java.com.wallet;

import main.java.com.wallet.model.InputCommandType;
import main.java.com.wallet.service.OfferService;
import main.java.com.wallet.service.WalletService;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.Scanner;

public class DigitalWalletSystem {

    public static void main(String[] args) {
        final ApplicationContext context = new ApplicationContext();
        final WalletService walletService = context.getWalletService();
        final OfferService offerService = context.getOfferService();

        Scanner scanner = null;
        try {
            File file = new File("input.txt");
            scanner = new Scanner(file);
            System.out.println("Reading input from file: " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.err.println("input.txt not found.");
            return;
        }

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;

            System.out.println("> " + line);

            String[] parts = line.trim().split("\\s+");
            String commandString = parts[0];

            try {
                InputCommandType command = InputCommandType.fromString(commandString);

                switch (command) {
                    case CREATE_WALLET:
                        walletService.createWallet(parts[1], new BigDecimal(parts[2]));
                        break;

                    case TRANSFER_MONEY:
                        walletService.transferMoney(parts[1], parts[2], new BigDecimal(parts[3]));
                        break;

                    case STATEMENT:
                        walletService.printStatement(parts[1]);
                        break;

                    case OVERVIEW:
                        walletService.printOverview();
                        break;

                    case OFFER2:
                        offerService.triggerOffer2();
                        break;

                    case FIXED_DEPOSIT:
                        walletService.createFixedDeposit(parts[1], new BigDecimal(parts[2]));
                        break;

                    case EXIT:
                        scanner.close();
                        System.exit(0);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error processing '" + line + "': " + e.getMessage());
                throw e;
            } catch (Exception e) {
                System.out.println("Error processing '" + line + "': " + e.getMessage());
            }
        }
        scanner.close();
    }
}