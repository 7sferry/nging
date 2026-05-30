package com.example.nging.user.config;

import com.example.nging.user.repository.create.CreateUserGatewayImpl;
import com.example.nging.user.repository.createcontact.CreateContactGatewayImpl;
import com.example.nging.user.repository.getall.GetAllUsersGatewayImpl;
import com.example.nging.user.repository.getbyid.GetUserByIdGatewayImpl;
import com.example.nging.user.repository.getcontact.GetContactGatewayImpl;
import com.example.nging.user.repository.jpa.ContactJpaRepository;
import com.example.nging.user.repository.jpa.UserJpaRepository;
import com.example.nging.user.repository.update.UpdateUserGatewayImpl;
import com.example.nging.user.repository.updatecontact.UpdateContactGatewayImpl;
import com.example.nging.user.usecase.create.CreateUserGateway;
import com.example.nging.user.usecase.create.CreateUserUseCase;
import com.example.nging.user.usecase.create.CreateUserUseCaseImpl;
import com.example.nging.user.usecase.createcontact.CreateContactGateway;
import com.example.nging.user.usecase.createcontact.CreateContactUseCase;
import com.example.nging.user.usecase.createcontact.CreateContactUseCaseImpl;
import com.example.nging.user.usecase.gateway.AccountingBalanceGateway;
import com.example.nging.user.usecase.getall.GetAllUsersGateway;
import com.example.nging.user.usecase.getall.GetAllUsersUseCase;
import com.example.nging.user.usecase.getall.GetAllUsersUseCaseImpl;
import com.example.nging.user.usecase.getbyid.GetUserByIdGateway;
import com.example.nging.user.usecase.getbyid.GetUserByIdUseCase;
import com.example.nging.user.usecase.getbyid.GetUserByIdUseCaseImpl;
import com.example.nging.user.usecase.getcontact.GetContactGateway;
import com.example.nging.user.usecase.getcontact.GetContactUseCase;
import com.example.nging.user.usecase.getcontact.GetContactUseCaseImpl;
import com.example.nging.user.usecase.update.UpdateUserGateway;
import com.example.nging.user.usecase.update.UpdateUserUseCase;
import com.example.nging.user.usecase.update.UpdateUserUseCaseImpl;
import com.example.nging.user.usecase.updatecontact.UpdateContactGateway;
import com.example.nging.user.usecase.updatecontact.UpdateContactUseCase;
import com.example.nging.user.usecase.updatecontact.UpdateContactUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {

    @Bean
    public GetAllUsersGateway getAllUsersGateway(UserJpaRepository repository) {
        return new GetAllUsersGatewayImpl(repository);
    }

    @Bean
    public GetAllUsersUseCase getAllUsersUseCase(GetAllUsersGateway gateway,
                                                  AccountingBalanceGateway accountingBalanceGateway) {
        return new GetAllUsersUseCaseImpl(gateway, accountingBalanceGateway);
    }

    @Bean
    public GetUserByIdGateway getUserByIdGateway(UserJpaRepository repository) {
        return new GetUserByIdGatewayImpl(repository);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(GetUserByIdGateway gateway,
                                                  AccountingBalanceGateway accountingBalanceGateway) {
        return new GetUserByIdUseCaseImpl(gateway, accountingBalanceGateway);
    }

    @Bean
    public GetContactGateway getContactGateway(ContactJpaRepository repository) {
        return new GetContactGatewayImpl(repository);
    }

    @Bean
    public GetContactUseCase getContactUseCase(GetContactGateway gateway) {
        return new GetContactUseCaseImpl(gateway);
    }

    @Bean
    public CreateUserGateway createUserGateway(UserJpaRepository repository) {
        return new CreateUserGatewayImpl(repository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(CreateUserGateway gateway) {
        return new CreateUserUseCaseImpl(gateway);
    }

    @Bean
    public UpdateUserGateway updateUserGateway(UserJpaRepository repository) {
        return new UpdateUserGatewayImpl(repository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UpdateUserGateway gateway) {
        return new UpdateUserUseCaseImpl(gateway);
    }

    @Bean
    public CreateContactGateway createContactGateway(ContactJpaRepository repository) {
        return new CreateContactGatewayImpl(repository);
    }

    @Bean
    public CreateContactUseCase createContactUseCase(CreateContactGateway gateway) {
        return new CreateContactUseCaseImpl(gateway);
    }

    @Bean
    public UpdateContactGateway updateContactGateway(ContactJpaRepository repository) {
        return new UpdateContactGatewayImpl(repository);
    }

    @Bean
    public UpdateContactUseCase updateContactUseCase(UpdateContactGateway gateway) {
        return new UpdateContactUseCaseImpl(gateway);
    }
}
