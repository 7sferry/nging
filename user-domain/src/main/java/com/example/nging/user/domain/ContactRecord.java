package com.example.nging.user.domain;

public record ContactRecord(
        Integer id,
        Integer userId,
        String phone,
        String address,
        String emergency
) {
}
