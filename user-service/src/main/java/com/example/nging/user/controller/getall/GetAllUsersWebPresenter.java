package com.example.nging.user.controller.getall;

import com.example.nging.user.domain.getall.GetAllUsersResponse;
import com.example.nging.user.domain.getall.GetAllUsersResult;
import com.example.nging.user.usecase.getall.GetAllUsersPresenter;
import lombok.Getter;

import java.util.List;

public class GetAllUsersWebPresenter implements GetAllUsersPresenter {

    @Getter
    private GetAllUsersResponse response;

    @Override
    public void present(GetAllUsersResult result) {
        List<GetAllUsersResponse.UserWithBalance> users = result.users().stream()
                .map(entry -> {
                    var user = entry.user();
                    var balance = entry.balance();
                    return new GetAllUsersResponse.UserWithBalance(
                            user.id(),
                            user.name(),
                            user.email(),
                            user.role(),
                            balance != null ? balance : "unavailable"
                    );
                })
                .toList();
        this.response = new GetAllUsersResponse(users);
    }
}
