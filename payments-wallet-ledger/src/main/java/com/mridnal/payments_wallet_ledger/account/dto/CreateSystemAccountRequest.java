package com.mridnal.payments_wallet_ledger.account.dto;

import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateSystemAccountRequest(
        @NotNull AccountType accountType
) {}
