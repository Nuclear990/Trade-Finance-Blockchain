package com.tradeAnchor.backend.service;

import com.tradeAnchor.backend.exception.WalletCreationException;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

@Service
public class VaultService {

    private final VaultTemplate vaultTemplate;

    public VaultService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public String generateWallet(Long userId) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
            // 1. Generate Ethereum keypair
            ECKeyPair keyPair = Keys.createEcKeyPair();
            // 2. Canonical 32-byte private key hex
            String privateKeyHex =
                    String.format("%064x", keyPair.getPrivateKey());

            // 3. Ethereum address
            String address =
                    "0x" + Keys.getAddress(keyPair.getPublicKey());
            // 4. Persist ONLY in Vault
            Map<String, Object> keyData = Map.of(
                    "privateKey", privateKeyHex,
                    "address", address
            );
        vaultTemplate
                .opsForVersionedKeyValue("eth-keys")
                .put("wallets/" + userId, keyData);

            return address;
    }
}
