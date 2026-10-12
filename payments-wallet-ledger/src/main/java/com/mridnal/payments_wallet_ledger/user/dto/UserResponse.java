package com.mridnal.payments_wallet_ledger.user.dto;

import com.mridnal.payments_wallet_ledger.common.enums.KycStatus;
import com.mridnal.payments_wallet_ledger.user.User;

import java.time.LocalDateTime;

public record UserResponse (
    Long userId,
    String name,
    String email,
    String phone,
    KycStatus kycStatus,
    LocalDateTime createdAt
){
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getKycStatus(),
                user.getCreatedAt()
        );
    }
}
