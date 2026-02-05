package com.tradeAnchor.backend.util;

import org.springframework.stereotype.Component;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

public final class EthAddressGenerator {

    private EthAddressGenerator() {}

    public static String generateAddress() {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            return "0x" + Keys.getAddress(keyPair.getPublicKey());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Ethereum address", e);
        }
    }
}
