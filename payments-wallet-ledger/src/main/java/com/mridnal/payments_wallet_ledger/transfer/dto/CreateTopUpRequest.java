package com.mridnal.payments_wallet_ledger.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTopUpRequest(
        @NotNull
        @Positive
        Long walletId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 18, fraction = 2)
        BigDecimal amount

    ) {}
