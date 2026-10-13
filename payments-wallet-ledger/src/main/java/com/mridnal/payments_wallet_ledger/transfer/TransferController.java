package com.mridnal.payments_wallet_ledger.transfer;

import com.mridnal.payments_wallet_ledger.transfer.dto.CreatePeerTransferRequest;
import com.mridnal.payments_wallet_ledger.transfer.dto.CreateTopUpRequest;
import com.mridnal.payments_wallet_ledger.transfer.dto.TransferResponse;
import com.mridnal.payments_wallet_ledger.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserService userService;

    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid  @RequestBody CreatePeerTransferRequest request,
                                     Authentication authentication) {
        String identity = authentication.getName();

        Long userId = userService.getUserIdByIdentity(identity);

        return transferService.createPeerTransfer(userId, request);
    }

    @PostMapping("/top-up")
    public TransferResponse topUp(
            @Valid @RequestBody CreateTopUpRequest request,
            Authentication authentication
    ) {
        Long userId = userService.getUserIdByIdentity(
                authentication.getName()
        );

        return transferService.createTopUpTransfer(userId, request);
    }
}
