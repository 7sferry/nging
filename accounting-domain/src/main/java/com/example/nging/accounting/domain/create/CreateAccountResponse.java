package com.example.nging.accounting.domain.create;

import java.math.BigDecimal;

public record CreateAccountResponse(Integer id, Integer userId, BigDecimal balance) {
}
