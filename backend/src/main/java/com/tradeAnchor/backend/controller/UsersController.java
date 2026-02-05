package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.CreateUserDto;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.UsersRepository;
import com.tradeAnchor.backend.service.BlockchainService;
import com.tradeAnchor.backend.util.EthAddressGenerator;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class UsersController {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlockchainService blockchainService;

    public UsersController(
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder,
            BlockchainService blockchainService
    ) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.blockchainService = blockchainService;
    }

    @PostMapping("/createUser")
    @Transactional
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserDto dto
    ) {

        /* ---------- DB user ---------- */

        Users user = new Users(
                dto.getUsername(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getUserType()
        );

        // Generate a NON-SIGNING Ethereum address
        String ethAddress = EthAddressGenerator.generateAddress();
        user.setEthereumAddress(ethAddress);

        user = usersRepository.save(user);

        /* ---------- On-chain identity ---------- */

        String txHash = blockchainService.createUser(
                ethAddress,
                user.getUserType()
        );
        System.out.println("CREATE USER TX = " + txHash);

        return ResponseEntity.status(201).build();
    }
}
