package com.example.nging.user.usecase.getbyid;

import com.example.nging.user.domain.UserRecord;

import java.util.Optional;

public interface GetUserByIdGateway {
    Optional<UserRecord> findById(Integer id);
}
