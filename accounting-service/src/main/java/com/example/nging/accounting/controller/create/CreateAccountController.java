package com.example.nging.accounting.controller.create;

import com.example.nging.accounting.domain.create.CreateAccountRequest;
import com.example.nging.accounting.usecase.create.CreateAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class CreateAccountController {

    private final CreateAccountUseCase useCase;

    @PostMapping({"", "/"})
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request) {
        CreateAccountWebPresenter presenter = new CreateAccountWebPresenter();
        useCase.execute(request, presenter);
        return presenter.getResponseEntity();
    }
}
