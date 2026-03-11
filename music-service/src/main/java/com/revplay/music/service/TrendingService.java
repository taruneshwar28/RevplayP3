package com.revplay.music.service;

import com.revplay.music.client.ArtistServiceClient;
import com.revplay.music.dto.PageResponse;
import com.revplay.music.dto.SongCatalogResponse;
import com.revplay.music.dto.TrendingResponse;
import com.revplay.music.exception.ServiceUnavailableException;
import feign.FeignException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrendingService {

    private final ArtistServiceClient artistServiceClient;

    public TrendingService(ArtistServiceClient artistServiceClient) {
        this.artistServiceClient = artistServiceClient;
    }

    @Cacheable(value = "trending", key = "#period + '-' + #limit")
    public TrendingResponse getTrendingSongs(TrendingResponse.TrendingPeriod period, int limit) {
        try {
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
            throw new ServiceUnavailableException("Artist service is currently unavailable", e);
        }
    }
}
