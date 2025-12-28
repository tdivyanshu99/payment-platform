package main.java.com.wallet.model;

import java.util.HashMap;
import java.util.Map;

public enum InputCommandType {
    CREATE_WALLET("CreateWallet"),
    TRANSFER_MONEY("TransferMoney"),
    STATEMENT("Statement"),
    OVERVIEW("Overview"),
    OFFER2("Offer2"),
    FIXED_DEPOSIT("FixedDeposit"),
    EXIT("Exit");

    private final String commandString;
    private final static Map<String, InputCommandType> mp = new HashMap<>();

    InputCommandType(String commandString) {
        System.out.println("Constructor " + commandString);
        this.commandString = commandString;
    }

    static {
        System.out.println("Static block");
        for (InputCommandType type : InputCommandType.values()) {
            mp.put(type.commandString, type);
        }
    }

    public static InputCommandType fromString(String text) {
        if (mp.containsKey(text)){
            return mp.get(text);
        }
        throw new IllegalArgumentException("Invalid command string: " + text);
    }
}