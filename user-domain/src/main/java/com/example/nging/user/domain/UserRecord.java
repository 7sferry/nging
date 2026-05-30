package com.example.nging.user.domain;

public record UserRecord(
        Integer id,
        String name,
        String email,
        String role
) {
}
