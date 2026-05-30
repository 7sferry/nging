package com.example.nging.user.usecase.createcontact;

import com.example.nging.user.domain.createcontact.CreateContactRequest;

public interface CreateContactUseCase {
    void execute(CreateContactRequest request, CreateContactPresenter presenter);
}
