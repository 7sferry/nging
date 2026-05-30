package com.example.nging.user.controller.getcontact;

import com.example.nging.user.domain.getcontact.GetContactRequest;
import com.example.nging.user.usecase.getcontact.GetContactUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class GetContactController {

    private final GetContactUseCase useCase;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getContact(@PathVariable int userId) {
        GetContactWebPresenter presenter = new GetContactWebPresenter();
        useCase.execute(new GetContactRequest(userId), presenter);
        return presenter.getResponseEntity();
    }
}
