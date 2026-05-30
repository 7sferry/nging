package com.example.nging.user.controller.updatecontact;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.updatecontact.UpdateContactRequest;
import com.example.nging.user.usecase.updatecontact.UpdateContactUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class UpdateContactController {

    public record UpdateContactBody(String phone, String address, String emergency) {
    }

    private final UpdateContactUseCase useCase;

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateContact(@PathVariable int userId,
                                           @RequestBody UpdateContactBody body,
                                           HttpServletRequest httpRequest) {
        log.info("updateContact userId={} authContext={}", userId, AuthContextLogger.extract(httpRequest));
        UpdateContactWebPresenter presenter = new UpdateContactWebPresenter();
        useCase.execute(new UpdateContactRequest(userId, body.phone(), body.address(), body.emergency()), presenter);
        return presenter.getResponseEntity();
    }
}
