package com.example.nging.user.repository.create;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.repository.entity.UserEntity;
import com.example.nging.user.repository.jpa.UserJpaRepository;
import com.example.nging.user.usecase.create.CreateUserGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateUserGatewayImpl implements CreateUserGateway {

    private final UserJpaRepository repository;

    @Override
    public UserRecord save(UserRecord user) {
        UserEntity entity = new UserEntity();
        entity.setName(user.name());
        entity.setEmail(user.email());
        entity.setRole(user.role());
        UserEntity saved = repository.save(entity);
        return new UserRecord(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
    }
}
