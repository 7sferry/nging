package com.example.nging.user.repository.updatecontact;

import com.example.nging.user.domain.ContactRecord;
import com.example.nging.user.repository.entity.ContactEntity;
import com.example.nging.user.repository.jpa.ContactJpaRepository;
import com.example.nging.user.usecase.updatecontact.UpdateContactGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UpdateContactGatewayImpl implements UpdateContactGateway {

    private final ContactJpaRepository repository;

    @Override
    public Optional<ContactRecord> updateByUserId(ContactRecord contact) {
        return repository.findByUserId(contact.userId())
                .map(entity -> {
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
                });
    }
}
