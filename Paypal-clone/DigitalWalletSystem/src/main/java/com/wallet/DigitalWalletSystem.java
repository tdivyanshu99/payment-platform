package main.java.com.wallet;

import main.java.com.wallet.model.InputCommandType;
import main.java.com.wallet.model.Wallet;
import main.java.com.wallet.repository.WalletRepository;
import main.java.com.wallet.service.OfferService;
import main.java.com.wallet.service.WalletService;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DigitalWalletSystem {

    public static void main(String[] args) {
        // Check if user wants to run performance test
        if (args.length > 0 && args[0].equals("--perf-test")) {
            runPerformanceTest();
            return;
        }

        // Normal operation with input.txt
        runNormalMode();
    }

    private static void runNormalMode() {
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

    /**
     * Performance test mode - validates 10K+ TPS
     */
    private static void runPerformanceTest() {
        System.out.println("Run perf test...");

        System.out.println("System Info:");
        System.out.println("  CPU Cores: " + Runtime.getRuntime().availableProcessors());
        System.out.println("  Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB\n");

        try {
            // Test 1: Baseline
            //runBaselineTest();

            // Test 2: Concurrent load
            //runConcurrentTest(50, 200);

            // Test 3: High load
            runConcurrentTest(70000, 2);

            // Test 4: Stress test
            //runStressTest(100000);

            System.out.println("\n✓ All tests completed successfully!");
        } catch (Exception e) {
            System.err.println("Performance test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runBaselineTest() {
        System.out.println("Run base test...");

        WalletRepository repo = new WalletRepository();
        WalletService service = new WalletService(repo);

        for (int i = 0; i < 100; i++) {
            service.createWallet("User" + i, new BigDecimal("1000"));
        }

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            int from = i % 100;
            int to = (i + 1) % 100;
            service.transferMoney("User" + from, "User" + to, new BigDecimal("1"));
        }

        long duration = System.currentTimeMillis() - start;
        double tps = 1000.0 / (duration / 1000.0);

        System.out.println("Transactions: 1,000");
        System.out.println("Duration: " + duration + " ms");
        System.out.println("TPS: " + String.format("%.0f", tps));
        System.out.println();
    }

    private static void runConcurrentTest(int numUsers, int transactionsPerUser) throws Exception {
        System.out.println("Run concurrent test...");

        WalletRepository repo = new WalletRepository();
        WalletService service = new WalletService(repo);

        System.out.print("Creating " + numUsers + " wallets... ");
        for (int i = 0; i < numUsers; i++) {
            service.createWallet("User" + i, new BigDecimal("10000"));
        }
        System.out.println("✓");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        int totalTransactions = numUsers * transactionsPerUser;
        CountDownLatch latch = new CountDownLatch(totalTransactions);

        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        System.out.print("Executing " + totalTransactions + " transactions with " + threadCount + " threads... ");

        long startTime = System.currentTimeMillis();
        Random random = new Random();

        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            for (int j = 0; j < transactionsPerUser; j++) {
                executor.submit(() -> {
                    long txStart = System.nanoTime();
                    try {
                        int toUser = random.nextInt(numUsers);
                        if (toUser == userId) {
                            toUser = (toUser + 1) % numUsers;
                        }

                        service.transferMoney(
                                "User" + userId,
                                "User" + toUser,
                                new BigDecimal("20")
                        );

                        successCount.incrementAndGet();
                        long txDuration = System.nanoTime() - txStart;
                        totalLatency.addAndGet(txDuration);
                    } catch (Exception e) {
                        //System.out.println("Received exception in executor: " + e.getMessage());
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("✓");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long duration = endTime - startTime;
        double tps = (totalTransactions * 1000.0) / duration;
        double avgLatency = (totalLatency.get() / successCount.get()) / 1_000_000.0;

        System.out.println("\nResults:");
        System.out.println("  Total Transactions: " + String.format("%,d", totalTransactions));
        System.out.println("  Successful: " + String.format("%,d", successCount.get()));
        System.out.println("  Failed: " + failCount.get());
        System.out.println("  Duration: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f TPS", tps));
        System.out.println("  Avg Latency: " + String.format("%.2f ms", avgLatency));

        if (tps >= 10000) {
            System.out.println("  Status: ✓ PASSED (≥10K TPS)");
        } else {
            System.out.println("  Status: ⚠ Below target (<10K TPS)");
        }

        verifyTotalBalance(repo, numUsers, new BigDecimal("10000"));
        System.out.println();
    }

    private static void runStressTest(int numTransactions) throws Exception {
        System.out.println("Run stress test...");

        WalletRepository repo = new WalletRepository();
        WalletService service = new WalletService(repo);

        int numUsers = 1000;
        System.out.print("Setting up " + numUsers + " wallets... ");
        for (int i = 0; i < numUsers; i++) {
            service.createWallet("User" + i, new BigDecimal("100000"));
        }
        System.out.println("✓");

        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numTransactions);

        int threadCount = Runtime.getRuntime().availableProcessors() * 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        System.out.print("Running stress test with " + threadCount + " threads... ");

        Random random = new Random();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numTransactions; i++) {
            executor.submit(() -> {
                try {
                    int from = random.nextInt(numUsers);
                    int to = random.nextInt(numUsers);
                    if (from == to) {
                        to = (to + 1) % numUsers;
                    }

                    service.transferMoney(
                            "User" + from,
                            "User" + to,
                            new BigDecimal("0.01")
                    );
                    completed.incrementAndGet();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("✓");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long duration = endTime - startTime;
        double tps = (numTransactions * 1000.0) / duration;

        System.out.println("\nResults:");
        System.out.println("  Total Transactions: " + String.format("%,d", numTransactions));
        System.out.println("  Completed: " + String.format("%,d", completed.get()));
        System.out.println("  Duration: " + duration + " ms");
        System.out.println("  Peak Throughput: " + String.format("%.0f TPS", tps));

        if (tps >= 10000) {
            System.out.println("  Status: ✓ PASSED (≥10K TPS)");
        } else {
            System.out.println("  Status: ⚠ MARGINAL (<10K TPS)");
        }
        System.out.println();
    }

    private static void verifyTotalBalance(WalletRepository repo, int numUsers, BigDecimal initialBalance) {
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (int i = 0; i < numUsers; i++) {
            Wallet w = repo.get("User" + i);
            totalBalance = totalBalance.add(w.getBalance());
        }

        BigDecimal expectedTotal = initialBalance.multiply(new BigDecimal(numUsers));

        System.out.println("\nBalance Verification:");
        System.out.println("  Expected: " + expectedTotal.stripTrailingZeros().toPlainString());
        System.out.println("  Actual: " + totalBalance.stripTrailingZeros().toPlainString());

        if (totalBalance.compareTo(expectedTotal) >= 0) {
            System.out.println("  Status: ✓ PASSED (includes rewards)");
        } else {
            System.out.println("  Status: ✗ FAILED (money lost!)");
            throw new RuntimeException("Balance verification failed!");
        }
    }
}