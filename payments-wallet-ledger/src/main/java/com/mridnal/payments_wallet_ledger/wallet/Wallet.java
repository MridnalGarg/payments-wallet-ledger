package com.mridnal.payments_wallet_ledger.wallet;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.common.enums.WalletStatus;
import com.mridnal.payments_wallet_ledger.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Each user can create multiple wallets e.g. Savings, Current etc.
 * but each wallet can be linked to a single account only.
 *
 * User will need to TOP_UP the wallet inorder to make the money transfers
 * and can choose to withdraw the amount back to their account when needed.
 *
 * Withdrawal may charge a minimum processing fee that will be stored under
 * FEE_REVENUE account (A system account handled by admin).
 *
 * A withdrawal request may take upto 24 hours to complete.
 */

@Entity
@Table(name="wallets")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Wallet {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long walletId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "account_id",
            nullable = false,
            unique = true
    )
    private Account account;

    private String name;

    @PositiveOrZero
    @Column(name = "balance", precision = 20, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",  nullable = false)
    private WalletStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    public Wallet(User user, Account account, String walletName){
        this.user = user;
        this.account = account;
        this.name = walletName;
        this.balance = BigDecimal.ZERO;
        this.status = WalletStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
