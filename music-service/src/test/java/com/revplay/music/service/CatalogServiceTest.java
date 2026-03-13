package com.revplay.music.service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.GenreResponse;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.exception.ServiceUnavailableException;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ArtistServiceClient artistServiceClient;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(artistServiceClient);
    }

    @Test
    void getPublicSongsShouldPassThroughResponse() {
        PageResponse<SongCatalogResponse> expected = pageOf(
                song(1L, "Believer", "Imagine Dragons", "Evolve", "Rock", 10L)
        );
        when(artistServiceClient.getPublicSongs(0, 20)).thenReturn(expected);

        PageResponse<SongCatalogResponse> response = catalogService.getPublicSongs(0, 20);

        assertEquals(expected.getContent().size(), response.getContent().size());
        assertEquals("Believer", response.getContent().get(0).getTitle());
    }

    @Test
    void getSongsByGenreShouldFilterIgnoringCase() {
        when(artistServiceClient.getPublicSongs(0, 10)).thenReturn(pageOf(
                song(1L, "Believer", "Imagine Dragons", "Evolve", "Rock", 10L),
                song(2L, "Perfect", "Ed Sheeran", "Divide", "Pop", 20L),
                song(3L, "Thunder", "Imagine Dragons", "Evolve", "rock", 30L)
        ));

        PageResponse<SongCatalogResponse> response = catalogService.getSongsByGenre("ROCK", 0, 10);

        assertEquals(2, response.getContent().size());
        assertEquals(2L, response.getTotalElements());
    }

    @Test
    void getGenresShouldAggregateCounts() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenReturn(pageOf(
                song(1L, "Believer", "Imagine Dragons", "Evolve", "Rock", 10L),
                song(2L, "Thunder", "Imagine Dragons", "Evolve", "Rock", 20L),
                song(3L, "Perfect", "Ed Sheeran", "Divide", "Pop", 30L),
                song(4L, "No Genre", "Unknown", "Singles", "", 40L)
        ));

        List<GenreResponse> response = catalogService.getGenres();
        response.sort(Comparator.comparing(GenreResponse::getName));

        assertEquals(2, response.size());
        assertEquals("Pop", response.get(0).getName());
        assertEquals(1L, response.get(0).getSongCount());
        assertEquals("Rock", response.get(1).getName());
        assertEquals(2L, response.get(1).getSongCount());
    }

    @Test
    void getSongByIdShouldWrapNotFoundAsServiceUnavailable() {
        when(artistServiceClient.getSongById(99L)).thenThrow(feignNotFoundException());

        ServiceUnavailableException exception = assertThrows(
                ServiceUnavailableException.class,
                () -> catalogService.getSongById(99L)
        );

        assertEquals("Song not found with id: 99", exception.getMessage());
    }

    private static PageResponse<SongCatalogResponse> pageOf(SongCatalogResponse... songs) {
        return PageResponse.<SongCatalogResponse>builder()
                .content(List.of(songs))
                .page(0)
                .pageSize(songs.length)
                .totalElements((long) songs.length)
                .totalPages(1)
                .build();
    }

    private static SongCatalogResponse song(
            Long id,
            String title,
            String artistName,
            String albumTitle,
            String genre,
            Long playCount
    ) {
        return SongCatalogResponse.builder()
                .id(id)
                .title(title)
                .artistId(id)
                .artistName(artistName)
                .albumTitle(albumTitle)
                .genre(genre)
                .duration(180)
                .fileUrl("https://example.test/" + id)
                .playCount(playCount)
                .build();
    }

    private static FeignException.NotFound feignNotFoundException() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/internal/songs/99",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return (FeignException.NotFound) FeignException.errorStatus(
                "artist-service",
                feign.Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(request)
                        .headers(Map.of())
                        .build()
        );
    }
}
