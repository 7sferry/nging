package com.example.nging.accounting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountingController {

    // Hardcoded account balances by user ID
    private static final Map<Integer, BigDecimal> BALANCES = Map.of(
            1, new BigDecimal("15000.50"),
            2, new BigDecimal("8250.75"),
            3, new BigDecimal("3420.00")
    );

    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalance(@PathVariable int userId) {
        BigDecimal balance = BALANCES.get(userId);
        if (balance == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No account found for user " + userId));
        }
        return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "balance", balance
        ));
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getAllBalances() {
        List<Map<String, Object>> result = BALANCES.entrySet().stream()
                .map(e -> Map.<String, Object>of("user_id", e.getKey(), "balance", e.getValue()))
                .toList();
        return ResponseEntity.ok(Map.of("accounts", result));
    }
}
