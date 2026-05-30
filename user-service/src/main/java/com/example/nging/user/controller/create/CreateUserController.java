package com.example.nging.user.controller.create;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.create.CreateUserRequest;
import com.example.nging.user.usecase.create.CreateUserUseCase;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class CreateUserController {

    private final CreateUserUseCase useCase;

    @PostMapping({"", "/"})
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request,
                                        HttpServletRequest httpRequest) {
        log.info("createUser email={} authContext={}", request.email(), AuthContextLogger.extract(httpRequest));
        CreateUserWebPresenter presenter = new CreateUserWebPresenter();
        useCase.execute(request, presenter);
        return presenter.getResponseEntity();
    }
}
