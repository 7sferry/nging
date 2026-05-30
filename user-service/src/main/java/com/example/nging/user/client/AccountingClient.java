package com.example.nging.user.client;

import com.example.nging.user.usecase.gateway.AccountingBalanceGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class AccountingClient implements AccountingBalanceGateway {

    private final RestClient restClient;

    public AccountingClient(@Value("${services.accounting.url}") String accountingUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(accountingUrl)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<BigDecimal> findBalanceByUserId(Integer userId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/accounts/balance/{userId}", userId)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.containsKey("balance")) {
                return Optional.of(new BigDecimal(response.get("balance").toString()));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
