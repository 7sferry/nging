package com.example.nging.user.usecase.updatecontact;

import com.example.nging.user.domain.updatecontact.UpdateContactRequest;

public interface UpdateContactUseCase {
    void execute(UpdateContactRequest request, UpdateContactPresenter presenter);
}
