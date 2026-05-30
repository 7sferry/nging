package com.example.nging.user.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public final class AuthContextLogger {

    private AuthContextLogger() {
    }

    public static Map<String, Object> extract(HttpServletRequest request) {
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
