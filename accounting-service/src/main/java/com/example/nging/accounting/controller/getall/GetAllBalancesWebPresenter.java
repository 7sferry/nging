package com.example.nging.accounting.controller.getall;

import com.example.nging.accounting.domain.getall.GetAllBalancesResponse;
import com.example.nging.accounting.domain.getall.GetAllBalancesResult;
import com.example.nging.accounting.usecase.getall.GetAllBalancesPresenter;
import lombok.Getter;

import java.util.List;

public class GetAllBalancesWebPresenter implements GetAllBalancesPresenter {

    @Getter
    private GetAllBalancesResponse response;

    @Override
    public void present(GetAllBalancesResult result) {
        List<GetAllBalancesResponse.AccountBalance> accounts = result.accounts().stream()
                .map(account -> new GetAllBalancesResponse.AccountBalance(
                        account.userId(),
                        account.balance()
                ))
                .toList();

        this.response = new GetAllBalancesResponse(accounts);
    }
}
