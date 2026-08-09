package com.capstone.aicontent.controller;

import com.capstone.aicontent.dto.CheckoutResponse;
import com.capstone.aicontent.dto.ConfirmPaymentRequest;
import com.capstone.aicontent.dto.UserResponse;
import com.capstone.aicontent.service.CurrentUserService;
import com.capstone.aicontent.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;
    private final CurrentUserService current;

    public PaymentController(PaymentService payments, CurrentUserService current) {
        this.payments = payments;
        this.current = current;
    }

    @PostMapping("/create-checkout-session")
    public CheckoutResponse checkout(Authentication auth) {
        return payments.createCheckout(current.get(auth));
    }

    @PostMapping("/create-order")
    public CheckoutResponse createOrder(Authentication auth) {
        return payments.createCheckout(current.get(auth));
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(Authentication auth, @Valid @RequestBody ConfirmPaymentRequest request) {
        var user = current.get(auth);
        payments.confirm(user, request);
        return Map.of("message", "Your Pro subscription is active.", "user", UserResponse.from(user));
    }
}
