package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.dto.TrendingResponse;
import com.revplay.music.exception.ServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrendingService {

    private static final Logger log = LoggerFactory.getLogger(TrendingService.class);

    private final ArtistServiceClient artistServiceClient;

    public TrendingService(ArtistServiceClient artistServiceClient) {
        this.artistServiceClient = artistServiceClient;
    }

    @Cacheable(value = "trending", key = "#period + '-' + #limit")
    public TrendingResponse getTrendingSongs(TrendingResponse.TrendingPeriod period, int limit) {
        try {
            log.debug("Computing trending songs: period={}, limit={}", period, limit);
            PageResponse<SongCatalogResponse> allSongs = artistServiceClient.getPublicSongs(0, 1000);

            List<SongCatalogResponse> trendingSongs = allSongs.getContent().stream()
                    .sorted(Comparator.comparing(
                            song -> song.getPlayCount() != null ? song.getPlayCount() : 0L,
                            Comparator.reverseOrder()
                    ))
                    .limit(limit)
                    .collect(Collectors.toList());

            return TrendingResponse.builder()
                    .songs(trendingSongs)
                    .period(period)
                    .build();
        } catch (FeignException e) {
            log.error("Failed to compute trending songs via artist-service: period={}, limit={}, status={}",
                    period, limit, e.status(), e);
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }
}
