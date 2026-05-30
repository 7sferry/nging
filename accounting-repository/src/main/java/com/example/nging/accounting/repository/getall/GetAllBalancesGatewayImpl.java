package com.example.nging.accounting.repository.getall;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.repository.entity.AccountEntity;
import com.example.nging.accounting.repository.jpa.AccountJpaRepository;
import com.example.nging.accounting.usecase.getall.GetAllBalancesGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetAllBalancesGatewayImpl implements GetAllBalancesGateway {

    private final AccountJpaRepository repository;

    @Override
    public List<AccountRecord> findAll() {
        return repository.findAll().stream()
                .map(this::toAccountRecord)
                .toList();
    }

    private AccountRecord toAccountRecord(AccountEntity entity) {
        return new AccountRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance()
        );
    }
}
