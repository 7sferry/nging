package com.example.nging.user.controller.create;

import com.example.nging.user.domain.create.CreateUserResponse;
import com.example.nging.user.domain.create.CreateUserResult;
import com.example.nging.user.usecase.create.CreateUserPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CreateUserWebPresenter implements CreateUserPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(CreateUserResult result) {
        var user = result.user();
        var body = new CreateUserResponse(user.id(), user.name(), user.email(), user.role());
        this.responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
