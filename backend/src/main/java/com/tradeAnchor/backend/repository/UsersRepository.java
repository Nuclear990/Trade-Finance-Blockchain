package com.tradeAnchor.backend.repository;

import com.tradeAnchor.backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEthereumAddress(String ethereumAddress);
    boolean existsByUsername(String username);
    List<Users> findByEthereumAddressIn(Set<String> ethereumAddresses);

}
