package com.example.nging.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/contacts")
public class UserContactController {

    private static final Map<Integer, Map<String, String>> CONTACTS = Map.of(
            1, Map.of("phone", "+1-555-0101", "address", "123 Main St, New York", "emergency", "+1-555-0901"),
            2, Map.of("phone", "+1-555-0102", "address", "456 Oak Ave, Chicago", "emergency", "+1-555-0902"),
            3, Map.of("phone", "+1-555-0103", "address", "789 Pine Rd, Seattle", "emergency", "+1-555-0903")
    );

    @GetMapping("/{userId}")
    public ResponseEntity<?> getContact(@PathVariable int userId) {
        Map<String, String> contact = CONTACTS.get(userId);
        if (contact == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No contact found for user " + userId));
        }
        return ResponseEntity.ok(Map.of(
                "user_id", userId,
                "contact", contact
        ));
    }
}
