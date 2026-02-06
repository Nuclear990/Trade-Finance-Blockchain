package com.tradeAnchor.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Bean
    public Web3j web3j() {
        if (rpcUrl != null && (rpcUrl.startsWith("wss://") || rpcUrl.startsWith("ws://"))) {
            try {
                org.web3j.protocol.websocket.WebSocketService webSocketService = new org.web3j.protocol.websocket.WebSocketService(
                        rpcUrl, true);
                webSocketService.connect();
                return Web3j.build(webSocketService);
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to WebSocket RPC", e);
            }
        }
        return Web3j.build(new HttpService(rpcUrl));
    }
}
