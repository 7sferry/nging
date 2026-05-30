package com.example.nging.user.usecase.getcontact;

import com.example.nging.user.domain.getcontact.GetContactResult;

public interface GetContactPresenter {
    void present(GetContactResult result);
    void presentNotFound(Integer userId);
}
