package com.mridnal.payments_wallet_ledger.wallet.dto;

import com.mridnal.payments_wallet_ledger.common.enums.WalletStatus;
import com.mridnal.payments_wallet_ledger.wallet.Wallet;

import java.time.LocalDateTime;

public record WalletResponse(
    Long id,
    Long userId,
    Long accountId,
    String name,
    WalletStatus walletStatus,
    LocalDateTime createdAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId(),
                wallet.getUser().getUserId(),
                wallet.getAccount().getAccountId(),
                wallet.getName(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }
}
