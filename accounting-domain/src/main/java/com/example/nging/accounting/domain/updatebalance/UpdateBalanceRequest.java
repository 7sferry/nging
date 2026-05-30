package com.example.nging.accounting.domain.updatebalance;

import java.math.BigDecimal;

public record UpdateBalanceRequest(Integer userId, BigDecimal balance) {
}
