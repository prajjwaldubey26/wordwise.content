package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.CheckoutResponse;
import com.capstone.aicontent.dto.ConfirmPaymentRequest;
import com.capstone.aicontent.entity.Payment;
import com.capstone.aicontent.entity.Subscription;
import com.capstone.aicontent.entity.SubscriptionPlan;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.repository.PaymentRepository;
import com.capstone.aicontent.repository.SubscriptionRepository;
import com.capstone.aicontent.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PaymentService {
    /** Pro plan amount in paise (₹499.00). */
    public static final long PRO_AMOUNT_PAISE = 49900L;
    public static final String PRO_CURRENCY = "INR";

    private final String keyId;
    private final String keySecret;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final SubscriptionRepository subscriptions;

    public PaymentService(
            @Value("${razorpay.key.id:}") String keyId,
            @Value("${razorpay.key.secret:}") String keySecret,
            UserRepository users,
            PaymentRepository payments,
            SubscriptionRepository subscriptions
    ) {
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
        this.users = users;
        this.payments = payments;
        this.subscriptions = subscriptions;
    }

    public CheckoutResponse createCheckout(User user) {
        requireConfigured();
        if (user.getSubscriptionPlan() == SubscriptionPlan.PRO) {
            throw new BadRequestException("You already have an active Pro plan.");
        }
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", PRO_AMOUNT_PAISE);
            orderRequest.put("currency", PRO_CURRENCY);
            orderRequest.put("receipt", "ww_pro_" + user.getId() + "_" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject()
                    .put("userId", String.valueOf(user.getId()))
                    .put("plan", "PRO")
                    .put("email", user.getEmail()));
            Order order = client.orders.create(orderRequest);
            return new CheckoutResponse(
                    keyId,
                    order.get("id"),
                    PRO_AMOUNT_PAISE,
                    PRO_CURRENCY,
                    "WordWise Pro",
                    "WordWise Pro — monthly demo"
            );
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Razorpay could not create an order. Check your test keys and try again.");
        }
    }

    public void confirm(User user, ConfirmPaymentRequest request) {
        requireConfigured();
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.razorpayOrderId());
            attributes.put("razorpay_payment_id", request.razorpayPaymentId());
            attributes.put("razorpay_signature", request.razorpaySignature());
            boolean valid = Utils.verifyPaymentSignature(attributes, keySecret);
            if (!valid) {
                throw new BadRequestException("Payment signature verification failed.");
            }

            String transactionId = request.razorpayPaymentId();
            if (!payments.existsByTransactionId(transactionId)) {
                Payment payment = new Payment();
                payment.setUser(user);
                payment.setAmount(BigDecimal.valueOf(PRO_AMOUNT_PAISE).movePointLeft(2));
                payment.setPlan("PRO");
                payment.setStatus("PAID");
                payment.setTransactionId(transactionId);
                payments.save(payment);

                Subscription subscription = new Subscription();
                subscription.setUser(user);
                subscription.setPlanName("PRO");
                subscription.setStartDate(Instant.now());
                subscription.setEndDate(Instant.now().plus(30, ChronoUnit.DAYS));
                subscription.setStatus("ACTIVE");
                subscriptions.save(subscription);
            }

            user.setSubscriptionPlan(SubscriptionPlan.PRO);
            users.save(user);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("We could not confirm this Razorpay payment.");
        }
    }

    private void requireConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new BadRequestException(
                    "Razorpay is not configured. Add RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET (test keys) before upgrading."
            );
        }
    }
}
