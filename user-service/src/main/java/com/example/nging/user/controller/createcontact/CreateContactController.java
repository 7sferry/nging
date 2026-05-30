package com.example.nging.user.controller.createcontact;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.createcontact.CreateContactRequest;
import com.example.nging.user.usecase.createcontact.CreateContactUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class CreateContactController {

    private final CreateContactUseCase useCase;

    @PostMapping({"", "/"})
    public ResponseEntity<?> createContact(@RequestBody CreateContactRequest request,
                                           HttpServletRequest httpRequest) {
        log.info("createContact userId={} authContext={}", request.userId(), AuthContextLogger.extract(httpRequest));
        CreateContactWebPresenter presenter = new CreateContactWebPresenter();
        useCase.execute(request, presenter);
        return presenter.getResponseEntity();
    }
}
