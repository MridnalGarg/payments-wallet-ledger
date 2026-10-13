package com.mridnal.payments_wallet_ledger.transfer;

import com.mridnal.payments_wallet_ledger.ledger_entry.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

//    List<LedgerEntry> findAllByTransfer_TransferId(Long transferId);
//
//    List<LedgerEntry> findAllByAccount_AccountId(Long accountId);
}
