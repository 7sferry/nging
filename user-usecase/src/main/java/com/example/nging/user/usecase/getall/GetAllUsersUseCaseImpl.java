package com.example.nging.user.usecase.getall;

import com.example.nging.user.domain.UserWithBalance;
import com.example.nging.user.domain.getall.GetAllUsersRequest;
import com.example.nging.user.domain.getall.GetAllUsersResult;
import com.example.nging.user.usecase.gateway.AccountingBalanceGateway;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetAllUsersUseCaseImpl implements GetAllUsersUseCase {

    private final GetAllUsersGateway gateway;
    private final AccountingBalanceGateway accountingBalanceGateway;

    @Override
    public void execute(GetAllUsersRequest request, GetAllUsersPresenter presenter) {
        List<UserWithBalance> users = gateway.findAll().stream()
                .map(user -> new UserWithBalance(
                        user,
                        accountingBalanceGateway.findBalanceByUserId(user.id()).orElse(null)
                ))
                .toList();
        presenter.present(new GetAllUsersResult(users));
    }
}
