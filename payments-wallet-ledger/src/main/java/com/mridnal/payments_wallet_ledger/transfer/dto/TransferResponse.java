package com.mridnal.payments_wallet_ledger.transfer.dto;

import com.mridnal.payments_wallet_ledger.common.enums.TransferStatus;
import com.mridnal.payments_wallet_ledger.common.enums.TransferType;
import com.mridnal.payments_wallet_ledger.transfer.Transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long transferId,
        TransferType type,
        TransferStatus status,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getType(),
                transfer.getStatus(),
                transfer.getSourceAccount().getAccountId(),
                transfer.getDestinationAccount().getAccountId(),
                transfer.getAmount(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }
}
