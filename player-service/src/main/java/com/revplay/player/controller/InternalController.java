package com.revplay.player.controller;

import com.revplay.player.dto.ListenerStatsDto;
import com.revplay.player.dto.PlayHistoryDto;
import com.revplay.player.entity.ListeningHistory;
import com.revplay.player.service.ListeningHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/history")
public class InternalController {

    private final ListeningHistoryService listeningHistoryService;

    public InternalController(ListeningHistoryService listeningHistoryService) {
        this.listeningHistoryService = listeningHistoryService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ListeningHistory>> getUserHistory(@PathVariable Long userId) {
        List<ListeningHistory> history = listeningHistoryService.getHistoryByUserId(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/song/{songId}")
    public ResponseEntity<List<PlayHistoryDto>> getSongHistory(
            @PathVariable Long songId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PlayHistoryDto> history = listeningHistoryService.getPlayHistoryBySongId(
                songId,
                startDate != null ? startDate.atStartOfDay() : null,
                endDate != null ? endDate.atTime(LocalTime.MAX) : null
        );
        return ResponseEntity.ok(history);
    }

    @GetMapping("/artist/{artistId}/listeners")
    public ResponseEntity<List<ListenerStatsDto>> getArtistListeners(@PathVariable Long artistId) {
        List<ListenerStatsDto> listenerStats = listeningHistoryService.getListenerStatsForArtist(artistId);
        return ResponseEntity.ok(listenerStats);
    }

    @GetMapping("/trends")
    public ResponseEntity<List<PlayHistoryDto>> getTrends(
            @RequestParam Long artistId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PlayHistoryDto> trends = listeningHistoryService.getPlayHistoryForArtist(
                artistId,
                startDate != null ? startDate.atStartOfDay() : null,
                endDate != null ? endDate.atTime(LocalTime.MAX) : null
        );
        return ResponseEntity.ok(trends);
    }
}
