package com.capstone.aicontent.dto;

import com.capstone.aicontent.entity.User;
public record UserResponse(Long id, String name, String email, String role, String subscriptionPlan) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getSubscriptionPlan().name()); }
}
