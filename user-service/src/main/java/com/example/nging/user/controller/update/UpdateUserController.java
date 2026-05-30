package com.example.nging.user.controller.update;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.update.UpdateUserRequest;
import com.example.nging.user.usecase.update.UpdateUserUseCase;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UpdateUserController {

    public record UpdateUserBody(String name, String email, String role) {
    }

    private final UpdateUserUseCase useCase;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id,
                                        @RequestBody UpdateUserBody body,
                                        HttpServletRequest httpRequest) {
        log.info("updateUser id={} authContext={}", id, AuthContextLogger.extract(httpRequest));
        UpdateUserWebPresenter presenter = new UpdateUserWebPresenter();
        useCase.execute(new UpdateUserRequest(id, body.name(), body.email(), body.role()), presenter);
        return presenter.getResponseEntity();
    }
}
