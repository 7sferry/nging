package com.example.nging.user.usecase.getbyid;

import com.example.nging.user.domain.getbyid.GetUserByIdResult;

public interface GetUserByIdPresenter {
    void present(GetUserByIdResult result);
    void presentNotFound(Integer id);
}
