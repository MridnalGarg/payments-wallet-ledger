package com.mridnal.payments_wallet_ledger.common.exceptions;

import org.springframework.http.HttpStatus;

public class ResponseStatusException extends RuntimeException {
    public ResponseStatusException(HttpStatus httpStatus, String message) {
        super(message);
    }
}
