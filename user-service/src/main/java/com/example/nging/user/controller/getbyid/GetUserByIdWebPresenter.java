package com.example.nging.user.controller.getbyid;

import com.example.nging.user.domain.getbyid.GetUserByIdResponse;
import com.example.nging.user.domain.getbyid.GetUserByIdResult;
import com.example.nging.user.usecase.getbyid.GetUserByIdPresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class GetUserByIdWebPresenter implements GetUserByIdPresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(GetUserByIdResult result) {
        var entry = result.userWithBalance();
        var user = entry.user();
        var balance = entry.balance();
        var body = new GetUserByIdResponse.UserWithBalance(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                balance != null ? balance : "unavailable"
        );
        this.responseEntity = ResponseEntity.ok(new GetUserByIdResponse(body));
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
