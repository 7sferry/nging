package com.example.nging.accounting.repository.updatebalance;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.repository.entity.AccountEntity;
import com.example.nging.accounting.repository.jpa.AccountJpaRepository;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalanceGateway;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
public class UpdateBalanceGatewayImpl implements UpdateBalanceGateway {

    private final AccountJpaRepository repository;

    @Override
    public Optional<AccountRecord> updateByUserId(Integer userId, BigDecimal balance) {
        return repository.findByUserId(userId)
                .map(entity -> {
                    entity.setBalance(balance);
                    AccountEntity saved = repository.save(entity);
                    return new AccountRecord(saved.getId(), saved.getUserId(), saved.getBalance());
                });
    }
}
