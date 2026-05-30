package com.example.nging.user.domain.updatecontact;

public record UpdateContactRequest(Integer userId, String phone, String address, String emergency) {
}
