package com.example.nging.user.usecase.getbyid;

import com.example.nging.user.domain.getbyid.GetUserByIdRequest;

public interface GetUserByIdUseCase {
    void execute(GetUserByIdRequest request, GetUserByIdPresenter presenter);
}
