package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SearchResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ArtistServiceClient artistServiceClient;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(artistServiceClient);
    }

    @Test
    void searchSongsShouldMatchTitleArtistAndAlbumWithPagination() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenReturn(pageOf(
                song(1L, "Believer", "Imagine Dragons", "Evolve", "Rock", 120L),
                song(2L, "Thunder", "Imagine Dragons", "Evolve", "Rock", 95L),
                song(3L, "Perfect", "Ed Sheeran", "Divide", "Pop", 80L)
        ));

        SearchResponse response = searchService.searchSongs("imagine", 0, 10);

        assertEquals(2L, response.getTotalResults());
        assertEquals(2, response.getSongs().size());
        assertEquals(List.of("Believer", "Thunder"),
                response.getSongs().stream().map(SongCatalogResponse::getTitle).toList());
    }

    @Test
    void searchSongsShouldBeCaseInsensitiveAndRespectPagination() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenReturn(pageOf(
                song(1L, "Believer", "Imagine Dragons", "Evolve", "Rock", 120L),
                song(2L, "Believer - Live", "Imagine Dragons", "Live", "Rock", 60L),
                song(3L, "Believer Remix", "DJ Test", "Believer Collection", "EDM", 40L)
        ));

        SearchResponse response = searchService.searchSongs("BELIEVER", 1, 1);

        assertEquals(3L, response.getTotalResults());
        assertEquals(1, response.getSongs().size());
        assertEquals("Believer - Live", response.getSongs().get(0).getTitle());
        assertEquals(1, response.getPage());
        assertEquals(1, response.getPageSize());
    }

    @Test
    void searchSongsShouldWrapFeignErrors() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenThrow(feignServerException());

        assertThrows(ServiceUnavailableException.class, () -> searchService.searchSongs("test", 0, 10));
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

    private static FeignException feignServerException() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/internal/songs/public",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return FeignException.errorStatus(
                "artist-service",
                feign.Response.builder()
                        .status(503)
                        .reason("Service Unavailable")
                        .request(request)
                        .headers(Map.of())
                        .build()
        );
    }
}
