package com.revplay.player.controller;

import com.revplay.player.dto.PlayRequest;
import com.revplay.player.dto.PlayResponse;
import com.revplay.player.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/play")
    public ResponseEntity<PlayResponse> play(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PlayRequest request) {
        PlayResponse response = playerService.play(userId, request.getSongId());
        return ResponseEntity.ok(response);
    }
}
