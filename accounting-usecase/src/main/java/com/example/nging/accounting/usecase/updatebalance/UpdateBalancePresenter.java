package com.example.nging.accounting.usecase.updatebalance;

import com.example.nging.accounting.domain.updatebalance.UpdateBalanceResult;

public interface UpdateBalancePresenter {
    void present(UpdateBalanceResult result);
    void presentNotFound(Integer userId);
}
