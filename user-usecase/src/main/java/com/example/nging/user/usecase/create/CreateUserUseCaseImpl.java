package com.example.nging.user.usecase.create;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.domain.create.CreateUserRequest;
import com.example.nging.user.domain.create.CreateUserResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final CreateUserGateway gateway;

    @Override
    public void execute(CreateUserRequest request, CreateUserPresenter presenter) {
        UserRecord saved = gateway.save(new UserRecord(null, request.name(), request.email(), request.role()));
        presenter.present(new CreateUserResult(saved));
    }
}
