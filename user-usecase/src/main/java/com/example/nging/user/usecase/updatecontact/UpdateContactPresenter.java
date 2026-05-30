package com.example.nging.user.usecase.updatecontact;

import com.example.nging.user.domain.updatecontact.UpdateContactResult;

public interface UpdateContactPresenter {
    void present(UpdateContactResult result);
    void presentNotFound(Integer userId);
}
