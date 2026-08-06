package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.*;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.exception.BadRequestException;
import com.capstone.aicontent.repository.UserRepository;
import com.capstone.aicontent.security.CustomUserDetailsService;
import com.capstone.aicontent.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final CustomUserDetailsService userDetails;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, CustomUserDetailsService userDetails) { this.users = users; this.encoder = encoder; this.jwt = jwt; this.userDetails = userDetails; }
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email().toLowerCase())) throw new BadRequestException("An account with this email already exists.");
        User user = new User(); user.setName(request.name().trim()); user.setEmail(request.email()); user.setPassword(encoder.encode(request.password())); users.save(user);
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().toLowerCase()).orElseThrow(() -> new BadRequestException("Invalid email or password."));
        if (!encoder.matches(request.password(), user.getPassword())) throw new BadRequestException("Invalid email or password.");
        return response(user);
    }
    private AuthResponse response(User user) { return new AuthResponse(jwt.generateToken(userDetails.loadUserByUsername(user.getEmail())), UserResponse.from(user)); }
}
