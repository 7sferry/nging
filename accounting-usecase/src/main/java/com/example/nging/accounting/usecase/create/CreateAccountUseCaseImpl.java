package com.example.nging.accounting.usecase.create;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.domain.create.CreateAccountRequest;
import com.example.nging.accounting.domain.create.CreateAccountResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateAccountUseCaseImpl implements CreateAccountUseCase {

    private final CreateAccountGateway gateway;

    @Override
    public void execute(CreateAccountRequest request, CreateAccountPresenter presenter) {
        AccountRecord saved = gateway.save(new AccountRecord(null, request.userId(), request.balance()));
        presenter.present(new CreateAccountResult(saved));
    }
}
