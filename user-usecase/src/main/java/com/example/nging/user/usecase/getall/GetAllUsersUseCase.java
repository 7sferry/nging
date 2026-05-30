package com.example.nging.user.usecase.getall;

import com.example.nging.user.domain.getall.GetAllUsersRequest;

public interface GetAllUsersUseCase {
    void execute(GetAllUsersRequest request, GetAllUsersPresenter presenter);
}
