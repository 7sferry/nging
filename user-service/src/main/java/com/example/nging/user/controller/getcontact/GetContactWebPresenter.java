package com.example.nging.user.controller.getcontact;

import com.example.nging.user.domain.getcontact.GetContactResponse;
import com.example.nging.user.domain.getcontact.GetContactResult;
import com.example.nging.user.usecase.getcontact.GetContactPresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class GetContactWebPresenter implements GetContactPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(GetContactResult result) {
        var contact = result.contact();
        var response = new GetContactResponse(
                contact.userId(),
                Map.of(
                        "phone", contact.phone(),
                        "address", contact.address(),
                        "emergency", contact.emergency()
                )
        );
        this.responseEntity = ResponseEntity.ok(response);
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
