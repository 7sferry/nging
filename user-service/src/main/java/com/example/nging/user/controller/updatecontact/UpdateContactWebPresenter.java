package com.example.nging.user.controller.updatecontact;

import com.example.nging.user.domain.updatecontact.UpdateContactResponse;
import com.example.nging.user.domain.updatecontact.UpdateContactResult;
import com.example.nging.user.usecase.updatecontact.UpdateContactPresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class UpdateContactWebPresenter implements UpdateContactPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(UpdateContactResult result) {
        var contact = result.contact();
        var body = new UpdateContactResponse(
                contact.id(),
                contact.userId(),
                contact.phone(),
                contact.address(),
                contact.emergency()
        );
        this.responseEntity = ResponseEntity.ok(body);
    }

    @Override
    public void presentNotFound(Integer userId) {
        this.responseEntity = ResponseEntity.status(404)
                .body(Map.of("error", "No contact found for user " + userId));
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
