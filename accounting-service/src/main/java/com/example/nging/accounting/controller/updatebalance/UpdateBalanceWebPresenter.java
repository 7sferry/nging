package com.example.nging.accounting.controller.updatebalance;

import com.example.nging.accounting.domain.updatebalance.UpdateBalanceResponse;
import com.example.nging.accounting.domain.updatebalance.UpdateBalanceResult;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalancePresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class UpdateBalanceWebPresenter implements UpdateBalancePresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(UpdateBalanceResult result) {
        var account = result.account();
        var body = new UpdateBalanceResponse(account.id(), account.userId(), account.balance());
        this.responseEntity = ResponseEntity.ok(body);
    }

    @Override
    public void presentNotFound(Integer userId) {
        this.responseEntity = ResponseEntity.status(404)
                .body(Map.of("error", "No account found for user " + userId));
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
