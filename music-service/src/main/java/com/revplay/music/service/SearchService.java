package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SearchRequest;
import com.revplay.music.dto.SearchResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.exception.ServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ArtistServiceClient artistServiceClient;

    public SearchService(ArtistServiceClient artistServiceClient) {
        this.artistServiceClient = artistServiceClient;
    }

    public SearchResponse searchSongs(String query, int page, int size) {
        try {
            log.debug("Searching songs: query={}, page={}, size={}", query, page, size);
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(0, 1000);

            String lowerQuery = query.toLowerCase();
            List<SongCatalogResponse> matchingSongs = allSongs.getContent().stream()
                    .filter(song -> matchesQuery(song, lowerQuery))
                    .collect(Collectors.toList());

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, matchingSongs.size());
            List<SongCatalogResponse> paginatedSongs = matchingSongs.subList(
                    Math.min(start, matchingSongs.size()),
                    end
            );

            return SearchResponse.builder()
                    .songs(paginatedSongs)
                    .totalResults((long) matchingSongs.size())
                    .page(page)
                    .pageSize(size)
                    .build();
        } catch (FeignException e) {
            log.error("Failed to search songs via artist-service: query={}, page={}, size={}, status={}",
                    query, page, size, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    public SearchResponse searchByGenre(String genre, int page, int size) {
        try {
            log.debug("Searching songs by genre: genre={}, page={}, size={}", genre, page, size);
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(0, 1000);

            List<SongCatalogResponse> matchingSongs = allSongs.getContent().stream()
                    .filter(song -> song.getGenre() != null &&
                            song.getGenre().equalsIgnoreCase(genre))
                    .collect(Collectors.toList());

            int start = page * size;
            int end = Math.min(start + size, matchingSongs.size());
            List<SongCatalogResponse> paginatedSongs = matchingSongs.subList(
                    Math.min(start, matchingSongs.size()),
                    end
            );

            return SearchResponse.builder()
                    .songs(paginatedSongs)
                    .totalResults((long) matchingSongs.size())
                    .page(page)
                    .pageSize(size)
                    .build();
        } catch (FeignException e) {
            log.error("Failed to search songs by genre via artist-service: genre={}, page={}, size={}, status={}",
                    genre, page, size, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    public SearchResponse advancedSearch(SearchRequest searchRequest, int page, int size) {
        try {
            log.debug("Performing advanced search: query={}, genre={}, artistName={}, page={}, size={}",
                    searchRequest.getQuery(), searchRequest.getGenre(), searchRequest.getArtistName(), page, size);
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(0, 1000);

            String lowerQuery = searchRequest.getQuery().toLowerCase();
            List<SongCatalogResponse> matchingSongs = allSongs.getContent().stream()
                    .filter(song -> {
                        boolean queryMatch = matchesQuery(song, lowerQuery);

                        boolean genreMatch = true;
                        if (searchRequest.getGenre() != null && !searchRequest.getGenre().isEmpty()) {
                            genreMatch = song.getGenre() != null &&
                                    song.getGenre().equalsIgnoreCase(searchRequest.getGenre());
                        }

                        boolean artistMatch = true;
                        if (searchRequest.getArtistName() != null && !searchRequest.getArtistName().isEmpty()) {
                            artistMatch = song.getArtistName() != null &&
                                    song.getArtistName().toLowerCase().contains(searchRequest.getArtistName().toLowerCase());
                        }

                        return queryMatch && genreMatch && artistMatch;
                    })
                    .collect(Collectors.toList());

            int start = page * size;
            int end = Math.min(start + size, matchingSongs.size());
            List<SongCatalogResponse> paginatedSongs = matchingSongs.subList(
                    Math.min(start, matchingSongs.size()),
                    end
            );

            return SearchResponse.builder()
                    .songs(paginatedSongs)
                    .totalResults((long) matchingSongs.size())
                    .page(page)
                    .pageSize(size)
                    .build();
        } catch (FeignException e) {
            log.error("Failed to perform advanced search via artist-service: query={}, genre={}, artistName={}, page={}, size={}, status={}",
                    searchRequest.getQuery(), searchRequest.getGenre(), searchRequest.getArtistName(), page, size, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    private boolean matchesQuery(SongCatalogResponse song, String query) {
        return (song.getTitle() != null && song.getTitle().toLowerCase().contains(query)) ||
               (song.getArtistName() != null && song.getArtistName().toLowerCase().contains(query)) ||
               (song.getAlbumTitle() != null && song.getAlbumTitle().toLowerCase().contains(query));
    }
}
