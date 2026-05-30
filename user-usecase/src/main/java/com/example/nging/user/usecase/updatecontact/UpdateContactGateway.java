package com.example.nging.user.usecase.updatecontact;

import com.example.nging.user.domain.ContactRecord;

import java.util.Optional;

public interface UpdateContactGateway {
    Optional<ContactRecord> updateByUserId(ContactRecord contact);
}
