package com.mridnal.payments_wallet_ledger.account;

import com.mridnal.payments_wallet_ledger.common.enums.AccountStatus;
import com.mridnal.payments_wallet_ledger.common.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT a
       FROM Account a
       WHERE a.accountId = :accountId
       """)
    Optional<Account> findByAccountIdForUpdate(
            @Param("accountId") Long accountId
    );

    Optional<Account> findByAccountTypeAndStatus(
            AccountType accountType,
            AccountStatus status
    );
}
