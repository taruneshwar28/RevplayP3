package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.dto.TrendingResponse;
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
class TrendingServiceTest {

    @Mock
    private ArtistServiceClient artistServiceClient;

    private TrendingService trendingService;

    @BeforeEach
    void setUp() {
        trendingService = new TrendingService(artistServiceClient);
    }

    @Test
    void getTrendingSongsShouldSortByPlayCountDescendingAndApplyLimit() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenReturn(pageOf(
                song(1L, "Low", 10L),
                song(2L, "High", 200L),
                song(3L, "Mid", 80L)
        ));

        TrendingResponse response = trendingService.getTrendingSongs(TrendingResponse.TrendingPeriod.WEEKLY, 2);

        assertEquals(TrendingResponse.TrendingPeriod.WEEKLY, response.getPeriod());
        assertEquals(List.of("High", "Mid"),
                response.getSongs().stream().map(SongCatalogResponse::getTitle).toList());
    }

    @Test
    void getTrendingSongsShouldTreatNullPlayCountAsZero() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenReturn(pageOf(
                song(1L, "Unknown", null),
                song(2L, "Popular", 5L)
        ));

        TrendingResponse response = trendingService.getTrendingSongs(TrendingResponse.TrendingPeriod.DAILY, 2);

        assertEquals("Popular", response.getSongs().get(0).getTitle());
        assertEquals("Unknown", response.getSongs().get(1).getTitle());
    }

    @Test
    void getTrendingSongsShouldWrapFeignErrors() {
        when(artistServiceClient.getPublicSongs(0, 1000)).thenThrow(feignServerException());

        assertThrows(
                ServiceUnavailableException.class,
                () -> trendingService.getTrendingSongs(TrendingResponse.TrendingPeriod.MONTHLY, 5)
        );
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

    private static SongCatalogResponse song(Long id, String title, Long playCount) {
        return SongCatalogResponse.builder()
                .id(id)
                .title(title)
                .artistId(id)
                .artistName("Artist " + id)
                .albumTitle("Album " + id)
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
