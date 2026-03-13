package com.revplay.artist.controller;

import com.revplay.artist.dto.AlbumRequest;
import com.revplay.artist.dto.AlbumResponse;
import com.revplay.artist.service.AlbumService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists/albums")
public class AlbumController {

    private static final Logger log = LoggerFactory.getLogger(AlbumController.class);

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping
    public ResponseEntity<AlbumResponse> createAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AlbumRequest request) {
        log.info("Received request to create album for userId={} title={}", userId, request.getTitle());
        AlbumResponse response = albumService.createAlbum(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AlbumResponse>> getAlbums(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Received request to fetch albums for userId={}", userId);
        List<AlbumResponse> albums = albumService.getAlbumsByArtist(userId);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> getAlbum(
            @PathVariable Long albumId) {
        log.info("Received request to fetch album albumId={}", albumId);
        AlbumResponse response = albumService.getAlbum(albumId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{albumId}")
    public ResponseEntity<AlbumResponse> updateAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long albumId,
            @Valid @RequestBody AlbumRequest request) {
        log.info("Received request to update album albumId={} for userId={}", albumId, userId);
        AlbumResponse response = albumService.updateAlbum(userId, albumId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<Void> deleteAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long albumId) {
        log.info("Received request to delete album albumId={} for userId={}", albumId, userId);
        albumService.deleteAlbum(userId, albumId);
        return ResponseEntity.noContent().build();
    }
}
