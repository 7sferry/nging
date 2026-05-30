package com.example.nging.user.usecase.create;

import com.example.nging.user.domain.create.CreateUserRequest;

public interface CreateUserUseCase {
    void execute(CreateUserRequest request, CreateUserPresenter presenter);
}
