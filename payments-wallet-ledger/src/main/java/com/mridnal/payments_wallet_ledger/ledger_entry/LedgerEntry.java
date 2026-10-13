package com.mridnal.payments_wallet_ledger.ledger_entry;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.common.enums.LedgerDirection;
import com.mridnal.payments_wallet_ledger.transfer.Transfer;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="ledger_entries")
@Getter
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "id")
    private Long LedgerEntryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private LedgerDirection direction;

    @Column(name = "amount",  nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(
            Transfer transfer,
            Account account,
            LedgerDirection direction,
            BigDecimal amount) {

        this.transfer = transfer;
        this.account = account;
        this.direction = direction;
        this.amount = amount;
    }

    public static LedgerEntry create(
            Transfer transfer,
            Account account,
            LedgerDirection direction,
            BigDecimal amount) {

        if (transfer == null || account == null
                || direction == null || amount == null) {
            throw new IllegalArgumentException(
                    "Ledger entry fields must not be null");
        }

        if (amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Ledger entry amount must be positive with at most 2 decimal places");
        }

        return new LedgerEntry(transfer, account, direction, amount);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
