package com.example.nging.user.usecase.getcontact;

import com.example.nging.user.domain.getcontact.GetContactRequest;

public interface GetContactUseCase {
    void execute(GetContactRequest request, GetContactPresenter presenter);
}
