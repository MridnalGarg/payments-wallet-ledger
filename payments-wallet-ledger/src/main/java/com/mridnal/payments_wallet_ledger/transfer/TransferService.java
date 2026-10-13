package com.mridnal.payments_wallet_ledger.transfer;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.account.AccountRepository;
import com.mridnal.payments_wallet_ledger.common.enums.*;
import com.mridnal.payments_wallet_ledger.common.exceptions.ResponseStatusException;
import com.mridnal.payments_wallet_ledger.ledger_entry.LedgerEntry;
import com.mridnal.payments_wallet_ledger.ledger_entry.LedgerEntryRepository;
import com.mridnal.payments_wallet_ledger.transfer.dto.CreatePeerTransferRequest;
import com.mridnal.payments_wallet_ledger.transfer.dto.CreateTopUpRequest;
import com.mridnal.payments_wallet_ledger.transfer.dto.TransferResponse;
import com.mridnal.payments_wallet_ledger.wallet.Wallet;
import com.mridnal.payments_wallet_ledger.wallet.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Service
public class TransferService {

    private static final Logger LOGGER = Logger.getLogger(TransferService.class.getName());

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public TransferResponse createPeerTransfer(
            Long userId,
            CreatePeerTransferRequest request
    ){

        if (request.sourceWalletId().equals(
                request.destinationWalletId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination wallets must not be same");
        }

        Wallet sourceWallet =
            walletRepository
                .findByWalletId(request.sourceWalletId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Source wallet not found"));

        Wallet destinationWallet =
            walletRepository
                .findByWalletId(request.destinationWalletId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination wallet not found"));

        LOGGER.info("Source User ID: "
                + sourceWallet.getUser().getUserId()
                + ", accessing user Id: "
                + userId);

        if (!sourceWallet.getUser().getUserId()
                .equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own the source wallet");
        }

        if (sourceWallet.getStatus() != WalletStatus.ACTIVE
                || destinationWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Both wallets must be active");
        }

        Long sourceAccountId = sourceWallet.getAccount().getAccountId();

        Long destinationAccountId = destinationWallet.getAccount().getAccountId();

        // Lock accounts in a consistent order to reduce deadlocks.
        Account[] lockedAccounts =
                Stream.of(
                        sourceAccountId,
                        destinationAccountId)
                .sorted()
                .map(id -> accountRepository
                        .findByAccountIdForUpdate(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found: " + id)))
                .toArray(Account[]::new);

        Account sourceAccount = findAccount(
                lockedAccounts, sourceAccountId);

        Account destinationAccount = findAccount(
                lockedAccounts, destinationAccountId);

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE
                || destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Both accounts must be active");
        }

        BigDecimal totalCredits =
                ledgerEntryRepository.sumAmountByAccountAndDirection(
                        sourceAccountId, LedgerDirection.CREDIT);

        BigDecimal totalDebits =
                ledgerEntryRepository.sumAmountByAccountAndDirection(
                        sourceAccountId, LedgerDirection.DEBIT);

        BigDecimal availableBalance =
                totalCredits.subtract(totalDebits);

        if (availableBalance.compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient wallet balance");
        }

        Transfer transfer = Transfer.create(
                TransferType.PEER_TRANSFER,
                sourceAccount,
                destinationAccount,
                request.amount(),
                null
        );

        // Create a transfer progress from source to destination
        transfer = transferRepository.saveAndFlush(transfer);

        // Create ledger entry for DEBIT from source account
        ledgerEntryRepository.save(
                LedgerEntry.create(
                        transfer,
                        sourceAccount,
                        LedgerDirection.DEBIT,
                        request.amount()
                )
        );

        // Create ledger entry for CREDIT to destination account
        ledgerEntryRepository.save(
                LedgerEntry.create(
                        transfer,
                        destinationAccount,
                        LedgerDirection.CREDIT,
                        request.amount()
                )
        );

        transfer.markCompleted();

        transfer = transferRepository.save(transfer);

        return TransferResponse.from(transfer);
    }

    private Account findAccount(
            Account[] accounts,
            Long accountId) {

        return Stream.of(accounts)
                .filter(account ->
                        account.getAccountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Locked account not found: " + accountId));
    }

    @Transactional
    public TransferResponse createTopUpTransfer(
            Long userId,
            CreateTopUpRequest request
    ){
        // Validate the amount.
        BigDecimal amount = request.amount();

        if (amount == null
                || amount.signum() <= 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be positive and have at most 2 decimal places"
            );
        }

        // Load the wallet and verify ownership.
        Wallet wallet = walletRepository.findByWalletId(request.walletId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        if (!wallet.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this wallet"
            );
        }

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Wallet is not active"
            );
        }

        Account walletAccount = wallet.getAccount();

        // Find the platform CASH_IN system account.
        Account cashInAccount =
                accountRepository.findByAccountTypeAndStatus(
                        AccountType.CASH_IN,
                        AccountStatus.ACTIVE
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Active CASH_IN system account not found"
                ));

        // Lock both accounts in ascending ID order.
        Long cashInId = cashInAccount.getAccountId();
        Long walletAccountId = walletAccount.getAccountId();

        Account[] lockedAccounts =
                Stream.of(cashInId,
                        walletAccountId)
                        .sorted()
                        .map(id -> accountRepository
                                .findByAccountIdForUpdate(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Account not found: " + id)))
                        .toArray(Account[]::new);

        Account lockedCashIn = findAccount(
                lockedAccounts, cashInId);

        Account lockedWalletAccount = findAccount(
                lockedAccounts, walletAccountId);

        lockedCashIn = accountRepository
                .findByAccountIdForUpdate(cashInId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "CashIn  Account was not found"
                ));

        lockedWalletAccount = accountRepository
                .findByAccountIdForUpdate(walletAccountId)
                .orElseThrow(() ->  new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet Account was not found"
                ));

        // Recheck account status after obtaining the locks.
        if (lockedCashIn.getAccountType() != AccountType.CASH_IN
                || lockedCashIn.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "CASH_IN account is not active"
            );
        }

        if (lockedWalletAccount.getAccountType() != AccountType.WALLET
                || lockedWalletAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Wallet account is not active"
            );
        }

        // Create the TOP_UP transfer.
        Transfer transfer = Transfer.create(
                TransferType.TOP_UP,
                lockedCashIn,
                lockedWalletAccount,
                amount,
                null
        );

        transfer = transferRepository.saveAndFlush(transfer);

        // Record balanced ledger entries.
        LedgerEntry debit = LedgerEntry.create(
                transfer,
                lockedCashIn,
                LedgerDirection.DEBIT,
                amount
        );

        LedgerEntry credit = LedgerEntry.create(
                transfer,
                lockedWalletAccount,
                LedgerDirection.CREDIT,
                amount
        );

        ledgerEntryRepository.saveAll(List.of(debit, credit));

        // Complete the transfer in the same transaction.
        transfer.markCompleted();
        transfer = transferRepository.save(transfer);

        return TransferResponse.from(transfer);

    }

}
