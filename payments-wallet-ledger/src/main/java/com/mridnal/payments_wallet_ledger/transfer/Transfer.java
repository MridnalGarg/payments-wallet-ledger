package com.mridnal.payments_wallet_ledger.transfer;

import com.mridnal.payments_wallet_ledger.account.Account;
import com.mridnal.payments_wallet_ledger.common.enums.FraudStatus;
import com.mridnal.payments_wallet_ledger.common.enums.TransferStatus;
import com.mridnal.payments_wallet_ledger.common.enums.TransferType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="transfers")
@Getter
public class Transfer {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transfer_id")
    private Transfer originalTransfer;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", nullable = false, length = 30)
    private FraudStatus fraudStatus;

    @Column(name = "fraud_reason", length = 500)
    private String fraudReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Creating a factory to create a transfer
     * without exposing the constructor
     */
    protected Transfer() {
    }

    private Transfer(
            TransferType type,
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            Transfer originalTransfer) {

        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.originalTransfer = originalTransfer;
        this.status = TransferStatus.PENDING;
        this.fraudStatus = FraudStatus.NOT_CHECKED;
    }

    public static Transfer create(
            TransferType type,
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            Transfer originalTransfer) {

        if (type == null || sourceAccount == null
                || destinationAccount == null || amount == null) {
            throw new IllegalArgumentException(
                    "Transfer fields must not be null");
        }

        if (amount.signum() <= 0
                || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must be positive and must be rounded off to 2 decimal places.");
        }

        if (sourceAccount.getAccountId()
                .equals(destinationAccount.getAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must differ");
        }

        return new Transfer(
                type,
                sourceAccount,
                destinationAccount,
                amount,
                originalTransfer);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = TransferStatus.FAILED;
    }

}
