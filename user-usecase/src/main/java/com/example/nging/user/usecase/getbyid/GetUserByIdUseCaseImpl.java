package com.example.nging.user.usecase.getbyid;

import com.example.nging.user.domain.UserWithBalance;
import com.example.nging.user.domain.getbyid.GetUserByIdRequest;
import com.example.nging.user.domain.getbyid.GetUserByIdResult;
import com.example.nging.user.usecase.gateway.AccountingBalanceGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUserByIdUseCaseImpl implements GetUserByIdUseCase {

    private final GetUserByIdGateway gateway;
    private final AccountingBalanceGateway accountingBalanceGateway;

    @Override
    public void execute(GetUserByIdRequest request, GetUserByIdPresenter presenter) {
        gateway.findById(request.id())
                .ifPresentOrElse(
                        user -> {
                            var balance = accountingBalanceGateway.findBalanceByUserId(user.id()).orElse(null);
                            presenter.present(new GetUserByIdResult(new UserWithBalance(user, balance)));
                        },
                        () -> presenter.presentNotFound(request.id())
                );
    }
}
