package com.example.nging.accounting.domain;

import java.math.BigDecimal;

public record AccountRecord(
        Integer id,
        Integer userId,
        BigDecimal balance
) {
}
