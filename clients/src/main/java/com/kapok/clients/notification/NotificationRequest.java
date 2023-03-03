package com.kapok.clients.notification;

import java.util.UUID;

public record NotificationRequest(
        UUID toCustomerId,
        String toCustomerName,
        String message
) {
}
