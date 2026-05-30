package com.example.nging.user.usecase.getcontact;

import com.example.nging.user.domain.ContactRecord;

import java.util.Optional;

public interface GetContactGateway {
    Optional<ContactRecord> findByUserId(Integer userId);
}
