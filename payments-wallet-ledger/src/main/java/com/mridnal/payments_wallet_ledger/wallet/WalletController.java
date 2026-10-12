package com.mridnal.payments_wallet_ledger.wallet;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.account.dto.AccountResponse;
import com.mridnal.payments_wallet_ledger.account.dto.CreateSystemAccountRequest;
import com.mridnal.payments_wallet_ledger.wallet.dto.CreateWalletRequest;
import com.mridnal.payments_wallet_ledger.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    @Autowired
    private WalletService walletService;

    /**
     * Create an account for a system by admin.
     */
    @PostMapping("/account/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createSystemAccount(
            @Valid @RequestBody CreateSystemAccountRequest request) {

        Account account = walletService.createSystemAccount(
                request.accountType()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountResponse.from(account));
    }

    /**
     * Create a wallet for a KYC-verified user.
     */
    @PostMapping("/wallet")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public WalletResponse createWallet(
            @Valid @RequestBody CreateWalletRequest request) {

        return walletService.createWallet(request);
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
}