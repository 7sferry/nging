package com.example.nging.user.usecase.create;

import com.example.nging.user.domain.UserRecord;

public interface CreateUserGateway {
    UserRecord save(UserRecord user);
}
