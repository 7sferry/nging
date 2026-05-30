package com.example.nging.accounting.repository.jpa;

import com.example.nging.accounting.repository.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Integer> {
    Optional<AccountEntity> findByUserId(Integer userId);
}
