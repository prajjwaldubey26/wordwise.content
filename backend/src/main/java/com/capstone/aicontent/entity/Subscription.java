package com.capstone.aicontent.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 20) private String planName;
    @Column(nullable = false) private Instant startDate;
    @Column(nullable = false) private Instant endDate;
    @Column(nullable = false, length = 30) private String status;
    public void setUser(User user) { this.user = user; } public void setPlanName(String planName) { this.planName = planName; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; } public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public void setStatus(String status) { this.status = status; }
}
