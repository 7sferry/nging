package com.example.nging.accounting.controller.updatebalance;

import com.example.nging.accounting.domain.updatebalance.UpdateBalanceRequest;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class UpdateBalanceController {

    public record UpdateBalanceBody(BigDecimal balance) {
    }

    private final UpdateBalanceUseCase useCase;

    @PutMapping("/balance/{userId}")
    public ResponseEntity<?> updateBalance(@PathVariable int userId,
                                           @RequestBody UpdateBalanceBody body) {
        UpdateBalanceWebPresenter presenter = new UpdateBalanceWebPresenter();
        useCase.execute(new UpdateBalanceRequest(userId, body.balance()), presenter);
        return presenter.getResponseEntity();
    }
}
