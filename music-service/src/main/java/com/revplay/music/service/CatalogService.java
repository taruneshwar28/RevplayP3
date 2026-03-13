package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.GenreResponse;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.exception.ServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final ArtistServiceClient artistServiceClient;

    public CatalogService(ArtistServiceClient artistServiceClient) {
        this.artistServiceClient = artistServiceClient;
    }

    public PageResponse<SongCatalogResponse> getPublicSongs(int page, int size) {
        try {
            log.debug("Fetching public songs from artist-service: page={}, size={}", page, size);
            PageResponse<SongCatalogResponse> response = artistServiceClient.getPublicSongs(page, size);
            log.debug("Fetched public songs from artist-service: page={}, size={}, returned={}",
                    page, size, response.getContent() != null ? response.getContent().size() : 0);
            return response;
        } catch (FeignException e) {
            log.error("Failed to fetch public songs from artist-service: page={}, size={}, status={}",
                    page, size, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    public SongCatalogResponse getSongById(Long songId) {
        try {
            log.debug("Fetching song by id from artist-service: songId={}", songId);
            SongCatalogResponse song = artistServiceClient.getSongById(songId);
            log.debug("Fetched song by id from artist-service: songId={}", songId);
            return song;
        } catch (FeignException.NotFound e) {
            log.warn("Song not found in artist-service: songId={}", songId);
            throw new ServiceUnavailableException("Song not found with id: " + songId);
        } catch (FeignException e) {
            log.error("Failed to fetch song by id from artist-service: songId={}, status={}", songId, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    public PageResponse<SongCatalogResponse> getSongsByGenre(String genre, int page, int size) {
        try {
            log.debug("Filtering songs by genre: genre={}, page={}, size={}", genre, page, size);
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(page, size);

            List<SongCatalogResponse> filteredSongs = allSongs.getContent().stream()
                    .filter(song -> song.getGenre() != null &&
                            song.getGenre().equalsIgnoreCase(genre))
                    .collect(Collectors.toList());

            return PageResponse.<SongCatalogResponse>builder()
                    .content(filteredSongs)
                    .page(page)
                    .pageSize(size)
                    .totalElements((long) filteredSongs.size())
                    .totalPages((int) Math.ceil((double) filteredSongs.size() / size))
                    .build();
        } catch (FeignException e) {
            log.error("Failed to fetch songs by genre from artist-service: genre={}, page={}, size={}, status={}",
                    genre, page, size, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }

    public List<GenreResponse> getGenres() {
        try {
            log.debug("Fetching genres from artist-service");
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(0, 1000);

            Map<String, Long> genreCounts = allSongs.getContent().stream()
                    .filter(song -> song.getGenre() != null && !song.getGenre().isEmpty())
                    .collect(Collectors.groupingBy(
                            SongCatalogResponse::getGenre,
                            Collectors.counting()
                    ));

            List<GenreResponse> genres = new ArrayList<>();
            genreCounts.forEach((genre, count) -> genres.add(GenreResponse.builder()
                    .name(genre)
                    .songCount(count)
                    .build()));

            log.debug("Computed genres from artist-service payload: count={}", genres.size());
            return genres;
        } catch (FeignException e) {
            log.error("Failed to fetch genres from artist-service: status={}", e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }
}
