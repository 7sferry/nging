package com.example.nging.accounting.repository.create;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.repository.entity.AccountEntity;
import com.example.nging.accounting.repository.jpa.AccountJpaRepository;
import com.example.nging.accounting.usecase.create.CreateAccountGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateAccountGatewayImpl implements CreateAccountGateway {

    private final AccountJpaRepository repository;

    @Override
    public AccountRecord save(AccountRecord account) {
        AccountEntity entity = new AccountEntity();
        entity.setUserId(account.userId());
        entity.setBalance(account.balance());
        AccountEntity saved = repository.save(entity);
        return new AccountRecord(saved.getId(), saved.getUserId(), saved.getBalance());
    }
}
