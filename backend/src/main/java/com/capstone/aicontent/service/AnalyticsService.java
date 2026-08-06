package com.capstone.aicontent.service;

import com.capstone.aicontent.dto.AdminStats;
import com.capstone.aicontent.dto.DashboardStats;
import com.capstone.aicontent.entity.SubscriptionPlan;
import com.capstone.aicontent.entity.User;
import com.capstone.aicontent.repository.ChapterSummaryRepository;
import com.capstone.aicontent.repository.ContentGenerationRepository;
import com.capstone.aicontent.repository.PlagiarismCheckRepository;
import com.capstone.aicontent.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final UserRepository users; private final ContentGenerationRepository generations; private final PlagiarismCheckRepository checks; private final ChapterSummaryRepository summaries;
    public AnalyticsService(UserRepository users, ContentGenerationRepository generations, PlagiarismCheckRepository checks, ChapterSummaryRepository summaries) { this.users = users; this.generations = generations; this.checks = checks; this.summaries = summaries; }
    public DashboardStats dashboard(User user) { Double average = checks.averageScoreByUserId(user.getId()); return new DashboardStats(generations.countByUserId(user.getId()), checks.countByUserId(user.getId()), summaries.countByUserId(user.getId()), average == null ? 0 : Math.round(average * 100.0) / 100.0); }
    public AdminStats admin() { return new AdminStats(users.count(), generations.count(), checks.count(), summaries.count(), users.countBySubscriptionPlan(SubscriptionPlan.FREE), users.countBySubscriptionPlan(SubscriptionPlan.PRO)); }
}
