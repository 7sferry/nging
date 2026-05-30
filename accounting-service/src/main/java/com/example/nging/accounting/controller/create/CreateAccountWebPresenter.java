package com.example.nging.accounting.controller.create;

import com.example.nging.accounting.domain.create.CreateAccountResponse;
import com.example.nging.accounting.domain.create.CreateAccountResult;
import com.example.nging.accounting.usecase.create.CreateAccountPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CreateAccountWebPresenter implements CreateAccountPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(CreateAccountResult result) {
        var account = result.account();
        var body = new CreateAccountResponse(account.id(), account.userId(), account.balance());
        this.responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
