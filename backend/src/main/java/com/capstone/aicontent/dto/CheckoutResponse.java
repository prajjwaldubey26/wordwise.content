package com.capstone.aicontent.dto;

public record CheckoutResponse(
        String keyId,
        String orderId,
        long amount,
        String currency,
        String planName,
        String description
) { }
