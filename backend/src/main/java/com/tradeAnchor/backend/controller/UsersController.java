package com.tradeAnchor.backend.controller;

import com.tradeAnchor.backend.dto.CreateUserDto;
import com.tradeAnchor.backend.dto.RefreshDto;
import com.tradeAnchor.backend.model.Users;
import com.tradeAnchor.backend.repository.UsersRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class UsersController {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersController(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @PostMapping("/createUser")
    @Transactional
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDto dto) {
        try {
            Users user = new Users();
            user.setUsername(dto.getUsername());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setUserType(dto.getUserType());

            usersRepository.save(user);
            return ResponseEntity.status(201).build();

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body("Username already taken");
        }
    }


}
