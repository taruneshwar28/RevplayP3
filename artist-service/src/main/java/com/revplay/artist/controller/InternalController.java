package com.revplay.artist.controller;

import com.revplay.artist.dto.ArtistProfileResponse;
import com.revplay.artist.service.ArtistService;
import com.revplay.artist.dto.SongResponse;
import com.revplay.artist.service.SongService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private static final Logger log = LoggerFactory.getLogger(InternalController.class);

    private final SongService songService;
    private final ArtistService artistService;

    public InternalController(SongService songService, ArtistService artistService) {
        this.songService = songService;
        this.artistService = artistService;
    }

    @GetMapping("/artists/by-user")
    public ResponseEntity<ArtistProfileResponse> getArtistByUserId(@RequestParam Long userId) {
        log.info("Received internal request to fetch artist by userId={}", userId);
        ArtistProfileResponse response = artistService.getArtistProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/songs/{songId}")
    public ResponseEntity<SongResponse> getSongById(@PathVariable Long songId) {
        log.info("Received internal request to fetch song by songId={}", songId);
        SongResponse response = songService.getSong(songId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/songs/batch")
    public ResponseEntity<List<SongResponse>> getSongsByIds(@RequestBody List<Long> ids) {
        log.info("Received internal batch song lookup for {} ids", ids.size());
        List<SongResponse> songs = songService.getSongsByIds(ids);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/songs/batch")
    public ResponseEntity<List<SongResponse>> getSongsByIdsFromQuery(@RequestParam List<Long> ids) {
        log.info("Received internal query-based batch song lookup for {} ids", ids.size());
        List<SongResponse> songs = songService.getSongsByIds(ids);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/songs/public")
    public ResponseEntity<Page<SongResponse>> getPublicSongs(Pageable pageable) {
        log.info("Received internal request to fetch public songs page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<SongResponse> songs = songService.getPublicSongs(pageable);
        return ResponseEntity.ok(songs);
    }

    @PostMapping("/songs/{songId}/play")
    public ResponseEntity<Void> incrementPlayCount(@PathVariable Long songId) {
        log.info("Received internal request to increment play count for songId={}", songId);
        songService.incrementPlayCount(songId);
        return ResponseEntity.ok().build();
    }
}
