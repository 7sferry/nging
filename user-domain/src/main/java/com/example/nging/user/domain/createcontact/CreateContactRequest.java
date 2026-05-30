package com.example.nging.user.domain.createcontact;

public record CreateContactRequest(Integer userId, String phone, String address, String emergency) {
}
