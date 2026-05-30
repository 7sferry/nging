package com.example.nging.accounting.usecase.getall;

import com.example.nging.accounting.domain.getall.GetAllBalancesRequest;

public interface GetAllBalancesUseCase {
    void execute(GetAllBalancesRequest request, GetAllBalancesPresenter presenter);
}
