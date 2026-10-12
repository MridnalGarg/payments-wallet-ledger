package com.mridnal.payments_wallet_ledger.user;

import com.mridnal.payments_wallet_ledger.common.exceptions.ResponseStatusException;
import com.mridnal.payments_wallet_ledger.common.enums.KycStatus;
import com.mridnal.payments_wallet_ledger.user.dto.CreateUserRequest;
import com.mridnal.payments_wallet_ledger.user.dto.UserResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {
    Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String name = request.name().trim();
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone is already registered"
            );
        }

        User user = new User(name, email, phone);

        logger.info("Creating user with name {} and email {} kyc_status {}",
                user.getName(),
                user.getEmail(),
                user.getKycStatus());

        try {
            User saved = userRepository.saveAndFlush(user);
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Handles a duplicate create request concurrently.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email or phone is already registered"
            );
        }
    }

    @Transactional
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse kycVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getKycStatus() == KycStatus.VERIFIED) {
            return UserResponse.from(user);
        }

        if (user.getKycStatus() == KycStatus.SUSPENDED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Suspended users cannot be verified"
            );
        }

        user.setKycStatus(KycStatus.VERIFIED);
        user.setUpdatedAt(LocalDateTime.now());
        return UserResponse.from(user);
    }
}
