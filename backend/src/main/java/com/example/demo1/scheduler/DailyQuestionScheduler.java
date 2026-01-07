package com.example.demo1.scheduler;

import com.example.demo1.service.DailyQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "daily-question.auto-generate.enabled", havingValue = "true", matchIfMissing = true)
public class DailyQuestionScheduler {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final DailyQuestionService dailyQuestionService;

    @Scheduled(cron = "${daily-question.auto-generate.cron:0 5 0 * * *}", zone = "Asia/Shanghai")
    public void generateTodayQuestionIfAbsent() {
        LocalDate today = LocalDate.now(ZONE_ID);
        try {
            dailyQuestionService.ensureQuestionExistsForDate(today);
        } catch (Exception e) {
            log.warn("每日一题自动出题失败，date={}", today, e);
        }
    }
}

