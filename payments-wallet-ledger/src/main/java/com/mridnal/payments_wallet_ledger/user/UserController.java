package com.mridnal.payments_wallet_ledger.user;

import com.mridnal.payments_wallet_ledger.user.dto.CreateUserRequest;
import com.mridnal.payments_wallet_ledger.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @PutMapping("/kyc-verification/{userId}")
    public UserResponse applyKycVerification(@PathVariable Long userId) {
        return userService.kycVerification(userId);
    }

}
