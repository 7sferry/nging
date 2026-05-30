package com.example.nging.accounting.domain.create;

import java.math.BigDecimal;

public record CreateAccountRequest(Integer userId, BigDecimal balance) {
}
