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
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at",  nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Account() {}

    protected Account(AccountType accountType, User user) {
        this.accountType = accountType;
        this.user = user;
        this.status = AccountStatus.ACTIVE;
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
