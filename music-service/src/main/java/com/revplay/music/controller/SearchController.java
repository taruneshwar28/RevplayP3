package com.revplay.music.controller;

import com.revplay.music.dto.SearchRequest;
import com.revplay.music.dto.SearchResponse;
import com.revplay.music.service.SearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchSongs(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Search request received: query={}, page={}, size={}", q, page, size);
        SearchResponse results = searchService.searchSongs(q, page, size);
        log.info("Search response ready: query={}, totalResults={}", q, results.getTotalResults());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/search/advanced")
    public ResponseEntity<SearchResponse> advancedSearch(
            @Valid @RequestBody SearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Advanced search request received: query={}, genre={}, artistName={}, page={}, size={}",
                searchRequest.getQuery(), searchRequest.getGenre(), searchRequest.getArtistName(), page, size);
        SearchResponse results = searchService.advancedSearch(searchRequest, page, size);
        log.info("Advanced search response ready: query={}, totalResults={}",
                searchRequest.getQuery(), results.getTotalResults());
        return ResponseEntity.ok(results);
    }
}
