package com.mridnal.payments_wallet_ledger.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByUserId(Long userId);

    Optional<User> findByUserId(Long userId);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.email) = LOWER(:identity)
           OR u.phone = :identity
        """)
    Optional<User> findByEmailOrPhone(
            @Param("identity") String identity);

}
