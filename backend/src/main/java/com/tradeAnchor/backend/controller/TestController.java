package com.tradeAnchor.backend.controller;


import com.tradeAnchor.backend.service.BlockchainService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secure")
public class TestController {
    private final BlockchainService blockchainService;

    public TestController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/hello")
    public String hello(){
        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return (blockchainService.userExists(username))? "user exists": "no such user on chain";
    }
}
