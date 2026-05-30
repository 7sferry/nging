package com.example.nging.accounting.usecase.create;

import com.example.nging.accounting.domain.AccountRecord;

public interface CreateAccountGateway {
    AccountRecord save(AccountRecord account);
}
