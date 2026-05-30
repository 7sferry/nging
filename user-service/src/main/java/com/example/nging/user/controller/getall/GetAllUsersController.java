package com.example.nging.user.controller.getall;

import com.example.nging.user.controller.AuthContextLogger;
import com.example.nging.user.domain.getall.GetAllUsersRequest;
import com.example.nging.user.usecase.getall.GetAllUsersUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class GetAllUsersController {

    private final GetAllUsersUseCase useCase;

    @GetMapping({"", "/"})
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        log.info("getAllUsers authContext={}", AuthContextLogger.extract(request));
        GetAllUsersWebPresenter presenter = new GetAllUsersWebPresenter();
        useCase.execute(new GetAllUsersRequest(), presenter);
        return ResponseEntity.ok(presenter.getResponse());
    }
}
