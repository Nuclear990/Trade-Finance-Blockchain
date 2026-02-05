package com.tradeAnchor.backend.model;

public enum UserType {
    BANK(0),
    COMPANY(1),
    SHIPPER(2);

    private final int chainValue;

    UserType(int chainValue) {
        this.chainValue = chainValue;
    }

    public int chainValue() {
        return chainValue;
    }

    public static UserType fromChainValue(int value) {
        for (UserType type : values()) {
            if (type.chainValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + value);
    }

}
