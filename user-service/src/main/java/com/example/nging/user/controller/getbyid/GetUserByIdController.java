package com.example.nging.user.controller.getbyid;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.getbyid.GetUserByIdRequest;
import com.example.nging.user.usecase.getbyid.GetUserByIdUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class GetUserByIdController {

    private final GetUserByIdUseCase useCase;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id, HttpServletRequest request) {
        log.info("getUserById id={} authContext={}", id, AuthContextLogger.extract(request));
        GetUserByIdWebPresenter presenter = new GetUserByIdWebPresenter();
        useCase.execute(new GetUserByIdRequest(id), presenter);
        return presenter.getResponseEntity();
    }
}
