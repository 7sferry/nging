package com.example.nging.accounting.controller.getbalance;

import com.example.nging.accounting.domain.getbalance.GetBalanceResponse;
import com.example.nging.accounting.domain.getbalance.GetBalanceResult;
import com.example.nging.accounting.usecase.getbalance.GetBalancePresenter;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class GetBalanceWebPresenter implements GetBalancePresenter {

    private ResponseEntity<?> responseEntity;

    @Override
    public void present(GetBalanceResult result) {
        var account = result.account();
        var response = new GetBalanceResponse(account.userId(), account.balance());
        this.responseEntity = ResponseEntity.ok(response);
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
