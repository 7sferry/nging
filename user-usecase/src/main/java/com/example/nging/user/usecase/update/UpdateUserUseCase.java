package com.example.nging.user.usecase.update;

import com.example.nging.user.domain.update.UpdateUserRequest;

public interface UpdateUserUseCase {
    void execute(UpdateUserRequest request, UpdateUserPresenter presenter);
}
