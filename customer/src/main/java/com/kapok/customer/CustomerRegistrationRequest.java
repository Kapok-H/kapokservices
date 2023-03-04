package com.kapok.customer;

import java.util.UUID;

public record CustomerRegistrationRequest(
        UUID id,
        String firstName,
        String lastName,
        Integer phoneNumber,
        String email
) {
}