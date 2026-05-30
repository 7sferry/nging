package com.example.nging.user.domain.getcontact;

import java.util.Map;

public record GetContactResponse(
        Integer userId,
        Map<String, String> contact
) {
}
