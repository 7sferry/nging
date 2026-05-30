package com.example.nging.accounting.usecase.getbalance;

import com.example.nging.accounting.domain.AccountRecord;

import java.util.Optional;

public interface GetBalanceGateway {
    Optional<AccountRecord> findByUserId(Integer userId);
}
