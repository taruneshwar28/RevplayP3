package com.revplay.player.service;

import com.revplay.player.client.ArtistServiceClient;
import com.revplay.player.dto.ListenerStatsDto;
import com.revplay.player.dto.ListeningHistoryResponse;
import com.revplay.player.dto.PlayHistoryDto;
import com.revplay.player.dto.SongDto;
import com.revplay.player.entity.ListeningHistory;
import com.revplay.player.repository.ListeningHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class ListeningHistoryService {

    private final ListeningHistoryRepository listeningHistoryRepository;
    private final ArtistServiceClient artistServiceClient;

    public ListeningHistoryService(ListeningHistoryRepository listeningHistoryRepository,
                                    ArtistServiceClient artistServiceClient) {
        this.listeningHistoryRepository = listeningHistoryRepository;
        this.artistServiceClient = artistServiceClient;
    }

    public Page<ListeningHistoryResponse> getRecentHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ListeningHistory> historyPage = listeningHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId, pageable);

        return historyPage.map(history -> ListeningHistoryResponse.builder()
                .id(history.getId())
                .songId(history.getSongId())
                .songTitle(defaultValue(null, history.getSongTitleSnapshot(), "Unknown"))
                .artistName(defaultValue(null, history.getArtistNameSnapshot(), "Unknown"))
                .albumTitle("Unknown")
                .coverImageUrl(null)
                .playedAt(history.getPlayedAt())
                .listenedDuration(history.getDuration())
                .build());
    }

    @Transactional
    public void clearHistory(Long userId) {
        listeningHistoryRepository.deleteByUserId(userId);
    }

    public List<ListeningHistory> getHistoryBySongId(Long songId) {
        return listeningHistoryRepository.findBySongId(songId);
    }

    public List<PlayHistoryDto> getPlayHistoryBySongId(Long songId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ListeningHistory> history = startDate != null && endDate != null
                ? listeningHistoryRepository.findBySongIdAndPlayedAtBetween(songId, startDate, endDate)
                : listeningHistoryRepository.findBySongId(songId);
        return mapPlayHistory(history);
    }

    public List<ListeningHistory> getHistoryByUserId(Long userId) {
        return listeningHistoryRepository.findByUserId(userId);
    }

    public Map<String, Long> getListenerCountsForSongs(List<Long> songIds) {
        List<Object[]> results = listeningHistoryRepository.findListenerCountsBySongIds(songIds);

        Map<String, Long> listenerCounts = new HashMap<>();
        for (Object[] result : results) {
            Long songId = (Long) result[0];
            Long listenerCount = (Long) result[1];
            listenerCounts.put(songId.toString(), listenerCount);
        }

        return listenerCounts;
    }

    public List<ListenerStatsDto> getListenerStatsForArtist(Long artistId) {
        List<SongDto> songs = artistServiceClient.getArtistSongs(artistId);
        List<Long> songIds = songs.stream()
                .map(SongDto::getId)
                .filter(Objects::nonNull)
                .toList();

        if (songIds.isEmpty()) {
            return List.of();
        }

        List<Object[]> results = listeningHistoryRepository.findTopListenersBySongIds(songIds);
        List<ListenerStatsDto> listeners = new ArrayList<>();

        for (Object[] result : results) {
            ListenerStatsDto dto = new ListenerStatsDto();
            dto.setUserId((Long) result[0]);
            dto.setPlayCount((Long) result[1]);

            Number totalDuration = (Number) result[2];
            double totalMinutes = totalDuration == null ? 0.0 : totalDuration.doubleValue() / 60.0;
            dto.setTotalListeningMinutes(Math.round(totalMinutes * 100.0) / 100.0);
            listeners.add(dto);
        }

        return listeners;
    }

    public List<PlayHistoryDto> getPlayHistoryForArtist(Long artistId, LocalDateTime startDate, LocalDateTime endDate) {
        List<SongDto> songs = artistServiceClient.getArtistSongs(artistId);
        List<Long> songIds = songs.stream()
                .map(SongDto::getId)
                .filter(Objects::nonNull)
                .toList();

        if (songIds.isEmpty()) {
            return List.of();
        }

        List<ListeningHistory> history = startDate != null && endDate != null
                ? listeningHistoryRepository.findBySongIdInAndPlayedAtBetween(songIds, startDate, endDate)
                : listeningHistoryRepository.findBySongIdIn(songIds);

        return mapPlayHistory(history);
    }

    public List<Map<String, Object>> getDailyTrends(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = listeningHistoryRepository.findDailyPlayCounts(startDate, endDate);

        List<Map<String, Object>> trends = new ArrayList<>();
        for (Object[] result : results) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("date", result[0]);
            trend.put("playCount", result[1]);
            trends.add(trend);
        }
        return trends;
    }

    private List<PlayHistoryDto> mapPlayHistory(List<ListeningHistory> history) {
        return history.stream()
                .map(this::toPlayHistoryDto)
                .toList();
    }

    private PlayHistoryDto toPlayHistoryDto(ListeningHistory history) {
        PlayHistoryDto dto = new PlayHistoryDto();
        dto.setId(history.getId());
        dto.setUserId(history.getUserId());
        dto.setSongId(history.getSongId());
        dto.setPlayedAt(history.getPlayedAt());
        dto.setListenDurationSeconds(history.getDuration());
        dto.setCompleted(Boolean.TRUE);
        return dto;
    }

    private String defaultValue(String primary, String fallback, String finalFallback) {
        String normalizedPrimary = normalize(primary);
        if (normalizedPrimary != null) {
            return normalizedPrimary;
        }

        String normalizedFallback = normalize(fallback);
        return normalizedFallback != null ? normalizedFallback : finalFallback;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
