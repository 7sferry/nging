package com.example.nging.accounting.usecase.getall;

import com.example.nging.accounting.domain.AccountRecord;

import java.util.List;

public interface GetAllBalancesGateway {
    List<AccountRecord> findAll();
}
