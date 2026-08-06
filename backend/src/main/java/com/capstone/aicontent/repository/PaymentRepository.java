package com.capstone.aicontent.repository;

import com.capstone.aicontent.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentRepository extends JpaRepository<Payment, Long> { boolean existsByTransactionId(String transactionId); }
