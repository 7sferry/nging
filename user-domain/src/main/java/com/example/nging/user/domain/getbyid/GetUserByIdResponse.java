package com.example.nging.user.domain.getbyid;

public record GetUserByIdResponse(UserWithBalance user) {
    public record UserWithBalance(
            Integer id,
            String name,
            String email,
            String role,
            Object accountBalance
    ) {
    }
}
