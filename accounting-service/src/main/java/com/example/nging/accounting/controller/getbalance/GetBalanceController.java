package com.example.nging.accounting.controller.getbalance;

import com.example.nging.accounting.domain.getbalance.GetBalanceRequest;
import com.example.nging.accounting.usecase.getbalance.GetBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class GetBalanceController {

    private final GetBalanceUseCase useCase;

    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalance(@PathVariable int userId) {
        GetBalanceWebPresenter presenter = new GetBalanceWebPresenter();
        useCase.execute(new GetBalanceRequest(userId), presenter);
        return presenter.getResponseEntity();
    }
}
