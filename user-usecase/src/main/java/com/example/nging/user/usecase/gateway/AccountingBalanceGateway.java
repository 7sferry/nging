package com.example.nging.user.usecase.gateway;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountingBalanceGateway {
    Optional<BigDecimal> findBalanceByUserId(Integer userId);
}
