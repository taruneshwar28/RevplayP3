package com.revplay.player.controller;

import com.revplay.player.dto.ListeningHistoryResponse;
import com.revplay.player.service.ListeningHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class ListeningHistoryController {

    private final ListeningHistoryService listeningHistoryService;

    public ListeningHistoryController(ListeningHistoryService listeningHistoryService) {
        this.listeningHistoryService = listeningHistoryService;
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<ListeningHistoryResponse>> getRecentHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ListeningHistoryResponse> history = listeningHistoryService.getRecentHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearHistory(
            @RequestHeader("X-User-Id") Long userId) {
        listeningHistoryService.clearHistory(userId);
        return ResponseEntity.ok(Map.of("message", "Listening history cleared successfully"));
    }
}
