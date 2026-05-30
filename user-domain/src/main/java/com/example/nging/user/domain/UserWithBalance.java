package com.example.nging.user.domain;

import java.math.BigDecimal;

public record UserWithBalance(UserRecord user, BigDecimal balance) {
}
