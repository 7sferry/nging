package com.example.nging.accounting.usecase.create;

import com.example.nging.accounting.domain.create.CreateAccountRequest;

public interface CreateAccountUseCase {
    void execute(CreateAccountRequest request, CreateAccountPresenter presenter);
}
