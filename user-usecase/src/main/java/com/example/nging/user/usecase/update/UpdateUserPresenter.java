package com.example.nging.user.usecase.update;

import com.example.nging.user.domain.update.UpdateUserResult;

public interface UpdateUserPresenter {
    void present(UpdateUserResult result);
    void presentNotFound(Integer id);
}
