package com.example.nging.user.usecase.update;

import com.example.nging.user.domain.UserRecord;

import java.util.Optional;

public interface UpdateUserGateway {
    Optional<UserRecord> update(UserRecord user);
}
