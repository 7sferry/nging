package com.example.nging.user.usecase.createcontact;

import com.example.nging.user.domain.ContactRecord;

public interface CreateContactGateway {
    ContactRecord save(ContactRecord contact);
}
