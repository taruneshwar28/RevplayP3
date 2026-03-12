package com.revplay.analytics.service;

import com.revplay.analytics.client.PlayerServiceClient;
import com.revplay.analytics.dto.DailyTrendData;
import com.revplay.analytics.dto.PlayHistoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregationServiceTest {

    @Mock
    private PlayerServiceClient playerServiceClient;

    private AggregationService aggregationService;

    @BeforeEach
    void setUp() {
        aggregationService = new AggregationService(playerServiceClient);
    }

    @Test
    void aggregateDailyStats_shouldSummarizeSingleDay() {
        LocalDate date = LocalDate.of(2026, 3, 10);
        when(playerServiceClient.getPlayTrends(7L, date, date.plusDays(1))).thenReturn(List.of(
                play(1L, 11L, date.atTime(10, 0), 120, true),
                play(2L, 11L, date.atTime(12, 0), 60, false),
                play(3L, 12L, date.atTime(13, 0), 30, true)
        ));

        DailyTrendData result = aggregationService.aggregateDailyStats(7L, date);

        assertEquals(date, result.getDate());
        assertEquals(3L, result.getPlayCount());
        assertEquals(2L, result.getUniqueListeners());
        assertEquals(3.5, result.getTotalMinutes());
    }

    @Test
    void calculateCompletionRate_shouldReturnRoundedPercentage() {
        when(playerServiceClient.getSongPlayHistory(5L, null, null)).thenReturn(List.of(
                play(1L, 11L, LocalDateTime.now(), 100, true),
                play(2L, 12L, LocalDateTime.now(), 90, false),
                play(3L, 13L, LocalDateTime.now(), 80, true)
        ));

        Double result = aggregationService.calculateCompletionRate(5L);

        assertEquals(66.67, result);
    }

    @Test
    void calculateCompletionRate_shouldReturnZeroWhenNoPlays() {
        when(playerServiceClient.getSongPlayHistory(5L, null, null)).thenReturn(List.of());

        Double result = aggregationService.calculateCompletionRate(5L);

        assertEquals(0.0, result);
    }

    @Test
    void calculateUniqueListeners_shouldDeduplicateUsersForPeriod() {
        LocalDate today = LocalDate.now();
        when(playerServiceClient.getPlayTrends(eq(9L), eq(today.minusWeeks(1)), eq(today))).thenReturn(List.of(
                play(1L, 20L, today.minusDays(5).atStartOfDay(), 50, true),
                play(2L, 20L, today.minusDays(4).atStartOfDay(), 55, true),
                play(3L, 21L, today.minusDays(1).atStartOfDay(), 60, true)
        ));

        Long result = aggregationService.calculateUniqueListeners(9L, "WEEKLY");

        assertEquals(2L, result);
    }

    @Test
    void aggregateTrends_shouldIncludeMissingDaysAsZeroRows() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 3);
        when(playerServiceClient.getPlayTrends(4L, start, end)).thenReturn(List.of(
                play(1L, 100L, start.atTime(8, 0), 120, true),
                play(2L, 101L, start.atTime(10, 30), 60, false),
                play(3L, 102L, end.atTime(9, 0), 30, true)
        ));

        List<DailyTrendData> trends = aggregationService.aggregateTrends(4L, start, end);

        assertEquals(3, trends.size());
        assertIterableEquals(List.of(start, start.plusDays(1), end), trends.stream().map(DailyTrendData::getDate).toList());
        assertEquals(2L, trends.get(0).getPlayCount());
        assertEquals(2L, trends.get(0).getUniqueListeners());
        assertEquals(3.0, trends.get(0).getTotalMinutes());
        assertEquals(0L, trends.get(1).getPlayCount());
        assertEquals(0L, trends.get(1).getUniqueListeners());
        assertEquals(0.0, trends.get(1).getTotalMinutes());
        assertEquals(1L, trends.get(2).getPlayCount());
        assertEquals(0.5, trends.get(2).getTotalMinutes());
    }

    @Test
    void helperMetrics_shouldCalculateCountsAndDurations() {
        List<PlayHistoryDto> plays = List.of(
                play(1L, 10L, LocalDateTime.now(), 60, true),
                play(2L, 10L, LocalDateTime.now(), 90, true),
                play(3L, 11L, LocalDateTime.now(), 30, false)
        );

        assertEquals(3L, aggregationService.countTotalPlays(plays));
        assertEquals(2L, aggregationService.countUniqueListeners(plays));
        assertEquals(3.0, aggregationService.calculateTotalListeningTime(plays));
        assertEquals(60.0, aggregationService.calculateAveragePlayDuration(plays));
    }

    private static PlayHistoryDto play(Long songId, Long userId, LocalDateTime playedAt, int duration, boolean completed) {
        PlayHistoryDto dto = new PlayHistoryDto();
        dto.setSongId(songId);
        dto.setUserId(userId);
        dto.setPlayedAt(playedAt);
        dto.setListenDurationSeconds(duration);
        dto.setCompleted(completed);
        return dto;
    }
}
