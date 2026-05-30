package com.example.nging.accounting.repository.getbalance;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.repository.entity.AccountEntity;
import com.example.nging.accounting.repository.jpa.AccountJpaRepository;
import com.example.nging.accounting.usecase.getbalance.GetBalanceGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class GetBalanceGatewayImpl implements GetBalanceGateway {

    private final AccountJpaRepository repository;

    @Override
    public Optional<AccountRecord> findByUserId(Integer userId) {
        return repository.findByUserId(userId)
                .map(this::toAccountRecord);
    }

    private AccountRecord toAccountRecord(AccountEntity entity) {
        return new AccountRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance()
        );
    }
}
