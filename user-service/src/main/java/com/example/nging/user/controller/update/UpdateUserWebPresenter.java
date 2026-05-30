package com.example.nging.user.controller.update;

import com.example.nging.user.domain.update.UpdateUserResponse;
import com.example.nging.user.domain.update.UpdateUserResult;
import com.example.nging.user.usecase.update.UpdateUserPresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class UpdateUserWebPresenter implements UpdateUserPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(UpdateUserResult result) {
        var user = result.user();
        var body = new UpdateUserResponse(user.id(), user.name(), user.email(), user.role());
        this.responseEntity = ResponseEntity.ok(body);
    }

    @Override
    public void presentNotFound(Integer id) {
        this.responseEntity = ResponseEntity.status(404)
                .body(Map.of("error", "User not found"));
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
