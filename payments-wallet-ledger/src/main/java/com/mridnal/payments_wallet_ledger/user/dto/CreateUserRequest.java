package com.mridnal.payments_wallet_ledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest (

    @NotBlank
    @Size(max = 150)
    String name,

    @NotBlank
    @Size(max = 320)
    String email,

    @NotBlank
    @Pattern(
            regexp = "^\\+[1-9]\\d{7,14}$",
            message = "Phone must use international format, e.g. +919876543210"
    )
    String phone
){}
