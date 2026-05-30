package com.example.nging.user.repository.update;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.repository.entity.UserEntity;
import com.example.nging.user.repository.jpa.UserJpaRepository;
import com.example.nging.user.usecase.update.UpdateUserGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class UpdateUserGatewayImpl implements UpdateUserGateway {

    private final UserJpaRepository repository;

    @Override
    public Optional<UserRecord> update(UserRecord user) {
        return repository.findById(user.id())
                .map(entity -> {
                    entity.setName(user.name());
                    entity.setEmail(user.email());
                    entity.setRole(user.role());
                    UserEntity saved = repository.save(entity);
                    return new UserRecord(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
                });
    }
}
