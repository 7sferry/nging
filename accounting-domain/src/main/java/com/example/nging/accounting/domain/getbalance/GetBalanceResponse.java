package com.example.nging.accounting.domain.getbalance;

import java.math.BigDecimal;

public record GetBalanceResponse(
        Integer userId,
        BigDecimal balance
) {
}
