package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.SubscriptionPlan;
import com.capstone.aicontent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countBySubscriptionPlan(SubscriptionPlan plan);
}
