package com.revplay.artist.controller;

import com.revplay.artist.dto.ArtistProfileRequest;
import com.revplay.artist.dto.ArtistProfileResponse;
import com.revplay.artist.dto.SongResponse;
import com.revplay.artist.service.ArtistService;
import com.revplay.artist.service.SongService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private static final Logger log = LoggerFactory.getLogger(ArtistController.class);

    private final ArtistService artistService;
    private final SongService songService;

    public ArtistController(ArtistService artistService, SongService songService) {
        this.artistService = artistService;
        this.songService = songService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ArtistProfileResponse> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Received request to fetch artist profile for userId={}", userId);
        ArtistProfileResponse response = artistService.getArtistProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile")
    public ResponseEntity<ArtistProfileResponse> createProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ArtistProfileRequest request) {
        log.info("Received request to create artist profile for userId={} with role={}", userId, role);

        if (!"ARTIST".equals(role)) {
            log.warn("Rejected artist profile creation for userId={} due to invalid role={}", userId, role);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        ArtistProfileResponse response = artistService.createArtist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<ArtistProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ArtistProfileRequest request) {
        log.info("Received request to update artist profile for userId={}", userId);
        ArtistProfileResponse response = artistService.updateArtist(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{artistId}")
    public ResponseEntity<ArtistProfileResponse> getArtistById(
            @PathVariable Long artistId) {
        log.info("Received request to fetch artist profile by artistId={}", artistId);
        ArtistProfileResponse response = artistService.getArtistById(artistId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{artistId}/songs")
    public ResponseEntity<List<SongResponse>> getSongsByArtistId(@PathVariable Long artistId) {
        log.info("Received request to fetch songs for artistId={}", artistId);
        List<SongResponse> songs = songService.getSongsByArtistId(artistId);
        return ResponseEntity.ok(songs);
    }
}
