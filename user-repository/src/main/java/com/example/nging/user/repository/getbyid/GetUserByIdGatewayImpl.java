package com.example.nging.user.repository.getbyid;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.repository.entity.UserEntity;
import com.example.nging.user.repository.jpa.UserJpaRepository;
import com.example.nging.user.usecase.getbyid.GetUserByIdGateway;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class GetUserByIdGatewayImpl implements GetUserByIdGateway {

    private final UserJpaRepository repository;

    @Override
    public Optional<UserRecord> findById(Integer id) {
        return repository.findById(id)
                .map(this::toUserRecord);
    }

    private UserRecord toUserRecord(UserEntity entity) {
        return new UserRecord(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole()
        );
    }
}
