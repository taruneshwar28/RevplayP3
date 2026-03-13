package com.revplay.music.controller;

import com.revplay.music.dto.GenreResponse;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/songs")
    public ResponseEntity<PageResponse<SongCatalogResponse>> getPublicSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Catalog request received for public songs: page={}, size={}", page, size);
        PageResponse<SongCatalogResponse> songs = catalogService.getPublicSongs(page, size);
        log.info("Catalog response ready for public songs: page={}, size={}, returned={}",
                page, size, songs.getContent() != null ? songs.getContent().size() : 0);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/songs/{songId}")
    public ResponseEntity<SongCatalogResponse> getSongById(@PathVariable Long songId) {
        log.info("Catalog request received for song: songId={}", songId);
        SongCatalogResponse song = catalogService.getSongById(songId);
        log.info("Catalog response ready for song: songId={}", songId);
        return ResponseEntity.ok(song);
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> getGenres() {
        log.info("Catalog request received for genre list");
        List<GenreResponse> genres = catalogService.getGenres();
        log.info("Catalog response ready for genre list: count={}", genres.size());
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/genres/{genre}/songs")
    public ResponseEntity<PageResponse<SongCatalogResponse>> getSongsByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Catalog request received for songs by genre: genre={}, page={}, size={}", genre, page, size);
        PageResponse<SongCatalogResponse> songs = catalogService.getSongsByGenre(genre, page, size);
        log.info("Catalog response ready for songs by genre: genre={}, returned={}",
                genre, songs.getContent() != null ? songs.getContent().size() : 0);
        return ResponseEntity.ok(songs);
    }
}
