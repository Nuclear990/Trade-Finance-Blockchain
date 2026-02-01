package com.tradeAnchor.backend.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    @Value("${spring.vault.uri}")
    private String vaultUri;

    @Value("${spring.vault.token-file}")
    private Resource tokenFile;

    @NotNull
    @Override
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(URI.create(vaultUri));
    }

    @NotNull
    @Override
    public ClientAuthentication clientAuthentication() {
        try {
            String token = Files.readString(tokenFile.getFile().toPath()).trim();
            return new TokenAuthentication(token);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Vault token", e);
        }
    }
}
