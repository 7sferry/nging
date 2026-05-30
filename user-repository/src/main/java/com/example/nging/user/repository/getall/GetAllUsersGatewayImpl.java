package com.example.nging.user.repository.getall;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.repository.entity.UserEntity;
import com.example.nging.user.repository.jpa.UserJpaRepository;
import com.example.nging.user.usecase.getall.GetAllUsersGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetAllUsersGatewayImpl implements GetAllUsersGateway {

    private final UserJpaRepository repository;

    @Override
    public List<UserRecord> findAll() {
        return repository.findAll().stream()
                .map(this::toUserRecord)
                .toList();
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
