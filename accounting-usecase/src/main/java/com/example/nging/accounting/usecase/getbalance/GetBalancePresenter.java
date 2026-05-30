package com.example.nging.accounting.usecase.getbalance;

import com.example.nging.accounting.domain.getbalance.GetBalanceResult;

public interface GetBalancePresenter {
    void present(GetBalanceResult result);
    void presentNotFound(Integer userId);
}
