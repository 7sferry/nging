package com.example.nging.user.repository.getcontact;

import com.example.nging.user.domain.ContactRecord;
import com.example.nging.user.repository.entity.ContactEntity;
import com.example.nging.user.repository.jpa.ContactJpaRepository;
import com.example.nging.user.usecase.getcontact.GetContactGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class GetContactGatewayImpl implements GetContactGateway {

    private final ContactJpaRepository repository;

    @Override
    public Optional<ContactRecord> findByUserId(Integer userId) {
        return repository.findByUserId(userId)
                .map(this::toContactRecord);
    }

    private ContactRecord toContactRecord(ContactEntity entity) {
        return new ContactRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getEmergency()
        );
    }
}
