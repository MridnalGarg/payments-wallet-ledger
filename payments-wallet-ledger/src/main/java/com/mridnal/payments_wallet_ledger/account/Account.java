package com.mridnal.payments_wallet_ledger.account;

import com.mridnal.payments_wallet_ledger.common.enums.AccountStatus;
import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import com.mridnal.payments_wallet_ledger.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="accounts")
@Getter
@Setter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private LocalDateTime createdAt;

    protected Account() {}

    protected Account(AccountType accountType, User user) {
        this.accountType = accountType;
        this.user = user;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    /** Creating a factory method
     * to allow creation of Wallet account by the user
     * without exposing the constructor
     */
    public static Account createWalletAccount(AccountType accountType, User user) {
        return new Account(
                accountType,
                user
        );
    }

    /** Allow creation of CASH_IN, CASH_OUT, FEE_REVENUE accounts
     * only by ADMIN without exposing the constructor
     */
    public static Account createSystemAccount(AccountType accountType) {
        if (accountType == null ||
                accountType == AccountType.WALLET) {
            throw new IllegalArgumentException(
                    "A system account must have a system account type");
        }

        return new Account(accountType, null);
    }
}
