package com.example.nging.accounting.usecase.getbalance;

import com.example.nging.accounting.domain.getbalance.GetBalanceRequest;
import com.example.nging.accounting.domain.getbalance.GetBalanceResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetBalanceUseCaseImpl implements GetBalanceUseCase {

    private final GetBalanceGateway gateway;

    @Override
    public void execute(GetBalanceRequest request, GetBalancePresenter presenter) {
        gateway.findByUserId(request.userId())
                .ifPresentOrElse(
                        account -> presenter.present(new GetBalanceResult(account)),
                        () -> presenter.presentNotFound(request.userId())
                );
    }
}
