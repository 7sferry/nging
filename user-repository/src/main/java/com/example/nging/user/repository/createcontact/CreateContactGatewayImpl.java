package com.example.nging.user.repository.createcontact;

import com.example.nging.user.domain.ContactRecord;
import com.example.nging.user.repository.entity.ContactEntity;
import com.example.nging.user.repository.jpa.ContactJpaRepository;
import com.example.nging.user.usecase.createcontact.CreateContactGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateContactGatewayImpl implements CreateContactGateway {

    private final ContactJpaRepository repository;

    @Override
    public ContactRecord save(ContactRecord contact) {
        ContactEntity entity = new ContactEntity();
        entity.setUserId(contact.userId());
        entity.setPhone(contact.phone());
        entity.setAddress(contact.address());
        entity.setEmergency(contact.emergency());
        ContactEntity saved = repository.save(entity);
        return new ContactRecord(
                saved.getId(),
                saved.getUserId(),
                saved.getPhone(),
                saved.getAddress(),
                saved.getEmergency()
        );
    }
}
