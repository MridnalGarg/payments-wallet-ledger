package com.mridnal.payments_wallet_ledger.wallet;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.account.AccountRepository;
import com.mridnal.payments_wallet_ledger.common.enums.*;
import com.mridnal.payments_wallet_ledger.common.exceptions.ResponseStatusException;
import com.mridnal.payments_wallet_ledger.ledger_entry.LedgerEntryRepository;
import com.mridnal.payments_wallet_ledger.user.User;
import com.mridnal.payments_wallet_ledger.user.UserRepository;
import com.mridnal.payments_wallet_ledger.wallet.dto.CreateWalletRequest;
import com.mridnal.payments_wallet_ledger.wallet.dto.WalletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public WalletResponse createWallet(Long userId, CreateWalletRequest request) {

        if (request.accountType() == null || request.accountType() != AccountType.WALLET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only Wallet account type is allowed");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getKycStatus() != KycStatus.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only KYC-verified users can create wallets"
            );
        }

        String walletName = request.name().trim();

        Account account = accountRepository.save(
                Account.createWalletAccount(request.accountType(), user)
        );

        Wallet wallet = new Wallet(user, account, walletName);

        Wallet savedWallet = walletRepository.save(wallet);

        return WalletResponse.from(savedWallet);
    }

    @Transactional
    public WalletResponse getWallet(Long walletId) {

        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        return WalletResponse.from(wallet);
    }

    @Transactional
    public List<WalletResponse> getWalletsForUser(Long userId) {

        if (!userRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        return walletRepository
                .findAllByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WalletResponse::from)
                .toList();
    }

    public BigDecimal getBalance(Long walletId) {

        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        if(wallet.getStatus() != WalletStatus.ACTIVE
                || wallet.getAccount().getStatus() != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Wallet Account is not active"
            );
        }

        BigDecimal totalCredits =
                ledgerEntryRepository.sumAmountByAccountAndDirection(
                        wallet.getAccount().getAccountId(), LedgerDirection.CREDIT);

        BigDecimal totalDebits =
                ledgerEntryRepository.sumAmountByAccountAndDirection(
                        wallet.getAccount().getAccountId(), LedgerDirection.DEBIT);

        return totalCredits.subtract(totalDebits);

    }

}
