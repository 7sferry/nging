package com.example.nging.accounting.usecase.getbalance;

import com.example.nging.accounting.domain.getbalance.GetBalanceRequest;

public interface GetBalanceUseCase {
    void execute(GetBalanceRequest request, GetBalancePresenter presenter);
}
