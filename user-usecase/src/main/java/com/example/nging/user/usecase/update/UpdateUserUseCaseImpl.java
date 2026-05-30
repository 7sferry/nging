package com.example.nging.user.usecase.update;

import com.example.nging.user.domain.UserRecord;
import com.example.nging.user.domain.update.UpdateUserRequest;
import com.example.nging.user.domain.update.UpdateUserResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateUserUseCaseImpl implements UpdateUserUseCase {

    private final UpdateUserGateway gateway;

    @Override
    public void execute(UpdateUserRequest request, UpdateUserPresenter presenter) {
        gateway.update(new UserRecord(request.id(), request.name(), request.email(), request.role()))
                .ifPresentOrElse(
                        user -> presenter.present(new UpdateUserResult(user)),
                        () -> presenter.presentNotFound(request.id())
                );
    }
}
