package com.example.nging.accounting.usecase.updatebalance;

import com.example.nging.accounting.domain.updatebalance.UpdateBalanceRequest;

public interface UpdateBalanceUseCase {
    void execute(UpdateBalanceRequest request, UpdateBalancePresenter presenter);
}
