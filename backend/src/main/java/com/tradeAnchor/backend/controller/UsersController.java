package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.CreateUserDto;
import com.tradeAnchor.backend.dto.RefreshDto;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.UsersRepository;
import com.tradeAnchor.backend.service.VaultService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@RestController
@RequestMapping("/public")
public class UsersController {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final VaultService vaultService;

    public UsersController(UsersRepository usersRepository, PasswordEncoder passwordEncoder, VaultService vaultService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.vaultService = vaultService;
    }
    @PostMapping("/createUser")
    @Transactional
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDto dto) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

        Users user = new Users(
                dto.getUsername(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getUserType()
        );
        user = usersRepository.save(user);
        String ethAddress = vaultService.generateWallet(user.getId());
        user.setEthereumAddress(ethAddress);
        usersRepository.save(user);
        return ResponseEntity.status(201).build();
    }

}
