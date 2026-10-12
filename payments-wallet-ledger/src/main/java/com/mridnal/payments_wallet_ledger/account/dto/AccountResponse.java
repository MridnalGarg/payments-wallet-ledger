package com.mridnal.payments_wallet_ledger.account.dto;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.common.enums.AccountStatus;
import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import com.mridnal.payments_wallet_ledger.user.User;

import java.time.LocalDateTime;

public record AccountResponse (
        Long accountId,
        AccountType accountType,
        User user,
        AccountStatus status,
        LocalDateTime createdAt

) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getAccountType(),
                account.getUser(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
