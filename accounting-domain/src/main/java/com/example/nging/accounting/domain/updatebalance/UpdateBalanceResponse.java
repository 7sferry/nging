package com.example.nging.accounting.domain.updatebalance;

import java.math.BigDecimal;

public record UpdateBalanceResponse(Integer id, Integer userId, BigDecimal balance) {
}
