package com.mridnal.payments_wallet_ledger.wallet.dto;

import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWalletRequest (
        @NotNull
        Long userId,

        @NotBlank
        @Size(max = 100)
        String name,

        AccountType accountType
){}
