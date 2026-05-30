package com.example.nging.user.usecase.getall;

import com.example.nging.user.domain.UserRecord;

import java.util.List;

public interface GetAllUsersGateway {
    List<UserRecord> findAll();
}
