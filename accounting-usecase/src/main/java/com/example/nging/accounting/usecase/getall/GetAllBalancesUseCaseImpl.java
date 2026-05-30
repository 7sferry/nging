package com.example.nging.accounting.usecase.getall;

import com.example.nging.accounting.domain.AccountRecord;
import com.example.nging.accounting.domain.getall.GetAllBalancesRequest;
import com.example.nging.accounting.domain.getall.GetAllBalancesResult;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GetAllBalancesUseCaseImpl implements GetAllBalancesUseCase {

    private final GetAllBalancesGateway gateway;

    @Override
    public void execute(GetAllBalancesRequest request, GetAllBalancesPresenter presenter) {
        List<AccountRecord> accounts = gateway.findAll();
        presenter.present(new GetAllBalancesResult(accounts));
    }
}
