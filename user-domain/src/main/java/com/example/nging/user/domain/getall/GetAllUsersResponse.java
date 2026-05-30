package com.example.nging.user.domain.getall;

import java.util.List;

public record GetAllUsersResponse(List<UserWithBalance> users) {
    public record UserWithBalance(
            Integer id,
            String name,
            String email,
            String role,
            Object accountBalance
    ) {
    }
}
