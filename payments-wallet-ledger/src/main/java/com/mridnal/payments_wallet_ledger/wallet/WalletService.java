package com.mridnal.payments_wallet_ledger.wallet;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.account.AccountRepository;
import com.mridnal.payments_wallet_ledger.common.exceptions.ResponseStatusException;
import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import com.mridnal.payments_wallet_ledger.common.enums.KycStatus;
import com.mridnal.payments_wallet_ledger.user.User;
import com.mridnal.payments_wallet_ledger.user.UserRepository;
import com.mridnal.payments_wallet_ledger.wallet.dto.CreateWalletRequest;
import com.mridnal.payments_wallet_ledger.wallet.dto.WalletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;
    @Transactional
    public Account createSystemAccount(AccountType accountType) {

        if (accountType == null || accountType == AccountType.WALLET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only system account types are allowed");
        }

        return accountRepository.save(
                Account.createSystemAccount(accountType)
        );
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {

        if (request.accountType() == null || request.accountType() != AccountType.WALLET) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only Wallet account type is allowed");
        }

        User user = userRepository.findByUserId(request.userId())
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

}
