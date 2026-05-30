package com.example.nging.accounting.domain.getall;

import java.math.BigDecimal;
import java.util.List;

public record GetAllBalancesResponse(List<AccountBalance> accounts) {

    public record AccountBalance(
            Integer userId,
            BigDecimal balance
    ) {
    }
}
