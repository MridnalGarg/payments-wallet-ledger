package com.mridnal.payments_wallet_ledger.wallet;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.account.dto.AccountResponse;
import com.mridnal.payments_wallet_ledger.account.dto.CreateSystemAccountRequest;
import com.mridnal.payments_wallet_ledger.common.exceptions.ResponseStatusException;
import com.mridnal.payments_wallet_ledger.user.UserService;
import com.mridnal.payments_wallet_ledger.wallet.dto.CreateWalletRequest;
import com.mridnal.payments_wallet_ledger.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;

    /**
     * Create a wallet for a KYC-verified user.
     */
    @PostMapping("/wallet")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public WalletResponse createWallet(
            @Valid @RequestBody CreateWalletRequest request,
            Authentication authentication) {
        String identity = authentication.getName();
        Long userId = userService.getUserIdByIdentity(
                authentication.getName()
        );

        if(userId == null){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not authorized to create wallet"
            );
        }

        return walletService.createWallet(userId, request);
    }

    /**
     * Retrieve a wallet by ID.
     */
    @GetMapping("/wallets/{walletId}")
    public WalletResponse getWallet(
            @PathVariable Long walletId) {

        return walletService.getWallet(walletId);
    }

    /**
     * Retrieve all wallets for the user.
     */
    @GetMapping("/users/{userId}/wallets")
    public List<WalletResponse> getWalletsForUser(
            @PathVariable Long userId) {

        return walletService.getWalletsForUser(userId);
    }

    /**
     * Get Wallet Balance
     */
    @GetMapping("/wallet/{walletId}/balance")
    public BigDecimal getBalance(@Valid @PathVariable Long walletId) {
        return walletService.getBalance(walletId);
    }
}