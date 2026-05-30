package com.example.nging.accounting.config;

import com.example.nging.accounting.repository.create.CreateAccountGatewayImpl;
import com.example.nging.accounting.repository.getall.GetAllBalancesGatewayImpl;
import com.example.nging.accounting.repository.getbalance.GetBalanceGatewayImpl;
import com.example.nging.accounting.repository.jpa.AccountJpaRepository;
import com.example.nging.accounting.repository.updatebalance.UpdateBalanceGatewayImpl;
import com.example.nging.accounting.usecase.create.CreateAccountGateway;
import com.example.nging.accounting.usecase.create.CreateAccountUseCase;
import com.example.nging.accounting.usecase.create.CreateAccountUseCaseImpl;
import com.example.nging.accounting.usecase.getall.GetAllBalancesGateway;
import com.example.nging.accounting.usecase.getall.GetAllBalancesUseCase;
import com.example.nging.accounting.usecase.getall.GetAllBalancesUseCaseImpl;
import com.example.nging.accounting.usecase.getbalance.GetBalanceGateway;
import com.example.nging.accounting.usecase.getbalance.GetBalanceUseCase;
import com.example.nging.accounting.usecase.getbalance.GetBalanceUseCaseImpl;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalanceGateway;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalanceUseCase;
import com.example.nging.accounting.usecase.updatebalance.UpdateBalanceUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountingConfig {

    @Bean
    public GetBalanceGateway getBalanceGateway(AccountJpaRepository repository) {
        return new GetBalanceGatewayImpl(repository);
    }

    @Bean
    public GetBalanceUseCase getBalanceUseCase(GetBalanceGateway gateway) {
        return new GetBalanceUseCaseImpl(gateway);
    }

    @Bean
    public GetAllBalancesGateway getAllBalancesGateway(AccountJpaRepository repository) {
        return new GetAllBalancesGatewayImpl(repository);
    }

    @Bean
    public GetAllBalancesUseCase getAllBalancesUseCase(GetAllBalancesGateway gateway) {
        return new GetAllBalancesUseCaseImpl(gateway);
    }

    @Bean
    public CreateAccountGateway createAccountGateway(AccountJpaRepository repository) {
        return new CreateAccountGatewayImpl(repository);
    }

    @Bean
    public CreateAccountUseCase createAccountUseCase(CreateAccountGateway gateway) {
        return new CreateAccountUseCaseImpl(gateway);
    }

    @Bean
    public UpdateBalanceGateway updateBalanceGateway(AccountJpaRepository repository) {
        return new UpdateBalanceGatewayImpl(repository);
    }

    @Bean
    public UpdateBalanceUseCase updateBalanceUseCase(UpdateBalanceGateway gateway) {
        return new UpdateBalanceUseCaseImpl(gateway);
    }
}
