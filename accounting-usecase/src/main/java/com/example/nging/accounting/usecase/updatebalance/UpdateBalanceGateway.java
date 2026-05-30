package com.example.nging.accounting.usecase.updatebalance;

import com.example.nging.accounting.domain.AccountRecord;

import java.math.BigDecimal;
import java.util.Optional;

public interface UpdateBalanceGateway {
    Optional<AccountRecord> updateByUserId(Integer userId, BigDecimal balance);
}
