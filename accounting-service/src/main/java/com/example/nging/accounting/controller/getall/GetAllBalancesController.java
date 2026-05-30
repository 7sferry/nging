package com.example.nging.accounting.controller.getall;

import com.example.nging.accounting.domain.getall.GetAllBalancesRequest;
import com.example.nging.accounting.usecase.getall.GetAllBalancesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class GetAllBalancesController {

    private final GetAllBalancesUseCase useCase;

    @GetMapping("/balances")
    public ResponseEntity<?> getAllBalances() {
        GetAllBalancesWebPresenter presenter = new GetAllBalancesWebPresenter();
        useCase.execute(new GetAllBalancesRequest(), presenter);
        return ResponseEntity.ok(presenter.getResponse());
    }
}
