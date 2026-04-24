package com.example.nging.user.controller;

import com.example.nging.user.client.AccountingClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountingClient accountingClient;

    private static final List<Map<String, Object>> USERS = List.of(
            Map.of("id", 1, "name", "John Doex12", "email", "john@example.com", "role", "admin"),
            Map.of("id", 2, "name", "Jane Smith blue", "email", "jane@example.com", "role", "user"),
            Map.of("id", 3, "name", "Bob Wilson", "email", "bob@example.com", "role", "user")
    );

    @GetMapping({"", "/"})
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        Map<String, Object> authContext = extractAuthContext(request);

        List<Map<String, Object>> usersWithBalance = USERS.stream()
                .map(user -> {
                    Map<String, Object> enriched = new HashMap<>(user);
                    BigDecimal balance = accountingClient.getBalance((int) user.get("id"));
                    enriched.put("account_balance", balance != null ? balance : "unavailable");
                    return enriched;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "auth", authContext,
                "users", usersWithBalance
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable int id, HttpServletRequest request) {
        Map<String, Object> authContext = extractAuthContext(request);
        return USERS.stream()
                .filter(u -> (int) u.get("id") == id)
                .findFirst()
                .<ResponseEntity<?>>map(user -> {
                    Map<String, Object> enriched = new HashMap<>(user);
                    BigDecimal balance = accountingClient.getBalance(id);
                    enriched.put("account_balance", balance != null ? balance : "unavailable");
                    return ResponseEntity.ok(Map.of(
                            "auth", authContext,
                            "user", enriched
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("error", "User not found")));
    }

    private Map<String, Object> extractAuthContext(HttpServletRequest request) {
        String roles = request.getHeader("X-Auth-Roles");
        String workEntities = request.getHeader("X-Auth-Work-Entities");
        return Map.of(
                "username", String.valueOf(request.getHeader("X-Auth-User")),
                "client_id", String.valueOf(request.getHeader("X-Auth-Client-Id")),
                "roles", roles != null ? List.of(roles.split(",")) : List.of(),
                "work_entities", workEntities != null ? List.of(workEntities.split(",")) : List.of()
        );
    }
}
