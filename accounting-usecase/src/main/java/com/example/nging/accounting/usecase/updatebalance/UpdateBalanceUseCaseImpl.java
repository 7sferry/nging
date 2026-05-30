package com.example.nging.accounting.usecase.updatebalance;

import com.example.nging.accounting.domain.updatebalance.UpdateBalanceRequest;
import com.example.nging.accounting.domain.updatebalance.UpdateBalanceResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateBalanceUseCaseImpl implements UpdateBalanceUseCase {

    private final UpdateBalanceGateway gateway;

    @Override
    public void execute(UpdateBalanceRequest request, UpdateBalancePresenter presenter) {
        gateway.updateByUserId(request.userId(), request.balance())
                .ifPresentOrElse(
                        account -> presenter.present(new UpdateBalanceResult(account)),
                        () -> presenter.presentNotFound(request.userId())
                );
    }
}
