package com.mridnal.payments_wallet_ledger.ledger_entry;

import com.mridnal.payments_wallet_ledger.common.enums.LedgerDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Integer> {
    @Query("""
       SELECT COALESCE(SUM(le.amount), 0)
       FROM LedgerEntry le
       WHERE le.account.accountId = :accountId
         AND le.direction = :direction
       """)
    BigDecimal sumAmountByAccountAndDirection(
            @Param("accountId") Long accountId,
            @Param("direction") LedgerDirection direction
    );
}
