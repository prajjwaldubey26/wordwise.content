package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.CheckoutResponse;
import com.capstone.aicontent.entity.Payment;
import com.capstone.aicontent.entity.Subscription;
import com.capstone.aicontent.entity.SubscriptionPlan;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.repository.PaymentRepository;
import com.capstone.aicontent.repository.SubscriptionRepository;
import com.capstone.aicontent.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PaymentService {
    private final String stripeKey, frontendUrl; private final UserRepository users; private final PaymentRepository payments; private final SubscriptionRepository subscriptions;
    public PaymentService(@Value("${stripe.secret.key:}") String stripeKey, @Value("${app.frontend-url}") String frontendUrl, UserRepository users, PaymentRepository payments, SubscriptionRepository subscriptions) { this.stripeKey = stripeKey; this.frontendUrl = frontendUrl; this.users = users; this.payments = payments; this.subscriptions = subscriptions; }
    public CheckoutResponse createCheckout(User user) {
        if (stripeKey.isBlank()) throw new BadRequestException("Stripe is not configured. Add your Stripe test secret key before upgrading.");
        try {
            Stripe.apiKey = stripeKey;
            SessionCreateParams params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/pricing")
                    .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency("usd").setUnitAmount(999L).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("AI Content Detector Pro — monthly demo").build()).build()).build()).build();
            Session session = Session.create(params); return new CheckoutResponse(session.getUrl());
        } catch (Exception e) { throw new BadRequestException("Stripe could not create a checkout session. Check your test key and try again."); }
    }
    public void confirm(User user, String sessionId) {
        if (stripeKey.isBlank()) throw new BadRequestException("Stripe is not configured.");
        try {
            Stripe.apiKey = stripeKey; Session session = Session.retrieve(sessionId);
            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) throw new BadRequestException("This Stripe Checkout session is not paid yet.");
            if (!payments.existsByTransactionId(sessionId)) {
                Payment payment = new Payment(); payment.setUser(user); payment.setAmount(new BigDecimal("9.99")); payment.setPlan("PRO"); payment.setStatus("PAID"); payment.setTransactionId(sessionId); payments.save(payment);
                Subscription subscription = new Subscription(); subscription.setUser(user); subscription.setPlanName("PRO"); subscription.setStartDate(Instant.now()); subscription.setEndDate(Instant.now().plus(30, ChronoUnit.DAYS)); subscription.setStatus("ACTIVE"); subscriptions.save(subscription);
            }
            user.setSubscriptionPlan(SubscriptionPlan.PRO); users.save(user);
        } catch (BadRequestException e) { throw e; }
        catch (Exception e) { throw new BadRequestException("We could not confirm this payment session."); }
    }
}
