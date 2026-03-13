package com.revplay.music.controller;

import com.revplay.music.dto.TrendingResponse;
import com.revplay.music.service.TrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class TrendingController {

    private static final Logger log = LoggerFactory.getLogger(TrendingController.class);

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/trending")
    public ResponseEntity<TrendingResponse> getTrendingSongs(
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Trending request received: period={}, limit={}", TrendingResponse.TrendingPeriod.WEEKLY, limit);
        TrendingResponse trending = trendingService.getTrendingSongs(
                TrendingResponse.TrendingPeriod.WEEKLY, limit);
        log.info("Trending response ready: period={}, returned={}", trending.getPeriod(), trending.getSongs().size());
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/trending/{period}")
    public ResponseEntity<TrendingResponse> getTrendingByPeriod(
            @PathVariable String period,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Trending request received: rawPeriod={}, limit={}", period, limit);
        TrendingResponse.TrendingPeriod trendingPeriod;
        try {
            trendingPeriod = TrendingResponse.TrendingPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trending period received, defaulting to WEEKLY: rawPeriod={}", period);
            trendingPeriod = TrendingResponse.TrendingPeriod.WEEKLY;
        }

        TrendingResponse trending = trendingService.getTrendingSongs(trendingPeriod, limit);
        log.info("Trending response ready: period={}, returned={}", trending.getPeriod(), trending.getSongs().size());
        return ResponseEntity.ok(trending);
    }
}
