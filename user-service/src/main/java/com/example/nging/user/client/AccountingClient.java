package com.example.nging.user.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AccountingClient {

    private final RestClient restClient;

    public AccountingClient(@Value("${services.accounting.url}") String accountingUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(accountingUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public BigDecimal getBalance(int userId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/accounts/balance/{userId}", userId)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.containsKey("balance")) {
                return new BigDecimal(response.get("balance").toString());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
