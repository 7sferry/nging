package com.example.nging.user.controller.createcontact;

import com.example.nging.user.domain.createcontact.CreateContactResponse;
import com.example.nging.user.domain.createcontact.CreateContactResult;
import com.example.nging.user.usecase.createcontact.CreateContactPresenter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CreateContactWebPresenter implements CreateContactPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(CreateContactResult result) {
        var contact = result.contact();
        var body = new CreateContactResponse(
                contact.id(),
                contact.userId(),
                contact.phone(),
                contact.address(),
                contact.emergency()
        );
        this.responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
