package com.revplay.artist.controller;

import com.revplay.artist.dto.SongResponse;
import com.revplay.artist.dto.SongUpdateRequest;
import com.revplay.artist.dto.SongUploadRequest;
import com.revplay.artist.entity.Visibility;
import com.revplay.artist.service.SongService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists/songs")
public class SongController {

    private static final Logger log = LoggerFactory.getLogger(SongController.class);

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping
    public ResponseEntity<SongResponse> uploadSong(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SongUploadRequest request) {
        log.info("Received request to upload song for userId={} title={}", userId, request.getTitle());
        SongResponse response = songService.uploadSong(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SongResponse>> getSongs(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Received request to fetch songs for userId={}", userId);
        List<SongResponse> songs = songService.getSongsByArtist(userId);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/{songId}")
    public ResponseEntity<SongResponse> getSong(
            @PathVariable Long songId) {
        log.info("Received request to fetch song songId={}", songId);
        SongResponse response = songService.getSong(songId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{songId}")
    public ResponseEntity<SongResponse> updateSong(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long songId,
            @Valid @RequestBody SongUpdateRequest request) {
        log.info("Received request to update song songId={} for userId={}", songId, userId);
        SongResponse response = songService.updateSong(userId, songId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> deleteSong(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long songId) {
        log.info("Received request to delete song songId={} for userId={}", songId, userId);
        songService.deleteSong(userId, songId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{songId}/visibility")
    public ResponseEntity<SongResponse> updateVisibility(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long songId,
            @RequestParam Visibility visibility) {
        log.info("Received request to update song visibility songId={} for userId={} visibility={}", songId, userId, visibility);
        SongUpdateRequest request = SongUpdateRequest.builder()
                .visibility(visibility)
                .build();
        SongResponse response = songService.updateSong(userId, songId, request);
        return ResponseEntity.ok(response);
    }
}
