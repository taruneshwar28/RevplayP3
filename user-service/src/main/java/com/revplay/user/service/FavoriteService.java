package com.revplay.user.service;

import com.revplay.user.client.ArtistServiceClient;
import com.revplay.user.dto.FavoriteCreateRequest;
import com.revplay.user.dto.FavoriteResponse;
import com.revplay.user.dto.SongDto;
import com.revplay.user.entity.Favorite;
import com.revplay.user.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ArtistServiceClient artistServiceClient;

    public FavoriteService(FavoriteRepository favoriteRepository,
                          ArtistServiceClient artistServiceClient) {
        this.favoriteRepository = favoriteRepository;
        this.artistServiceClient = artistServiceClient;
    }

    @Transactional
    public void addFavorite(Long userId, Long songId, FavoriteCreateRequest request) {
        if (favoriteRepository.existsByUserIdAndSongId(userId, songId)) {
            return;
        }

        Favorite favorite = Favorite.builder()
                .userId(userId)
                .songId(songId)
                .songTitleSnapshot(normalize(request != null ? request.getSongTitle() : null))
                .artistNameSnapshot(normalize(request != null ? request.getArtistName() : null))
                .build();

        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long songId) {
        Favorite favorite = favoriteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (favorite == null) {
            return;
        }

        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> songIds = favorites.stream()
                .map(Favorite::getSongId)
                .collect(Collectors.toList());

        List<SongDto> songs;
        try {
            songs = artistServiceClient.getSongsByIds(songIds);
        } catch (Exception e) {
            // If artist service is down, return favorites without song details
            System.err.println("Failed to fetch song details: " + e.getMessage());
            return favorites.stream()
                    .map(favorite -> FavoriteResponse.builder()
                            .id(favorite.getId())
                            .songId(favorite.getSongId())
                            .songTitle("Unavailable")
                            .artistName("Unavailable")
                            .addedAt(favorite.getAddedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        Map<Long, SongDto> songMap = songs.stream()
                .collect(Collectors.toMap(SongDto::getId, song -> song));

        return favorites.stream()
                .map(favorite -> {
                    SongDto song = songMap.get(favorite.getSongId());
                    if (song == null) {
                        try {
                            song = artistServiceClient.getSongById(favorite.getSongId());
                        } catch (Exception e) {
                            song = null;
                        }
                    }
                    return FavoriteResponse.builder()
                            .id(favorite.getId())
                            .songId(favorite.getSongId())
                            .songTitle(song != null && normalize(song.getTitle()) != null
                                    ? song.getTitle()
                                    : defaultValue(favorite.getSongTitleSnapshot(), "Unknown"))
                            .artistName(song != null && normalize(song.getArtistName()) != null
                                    ? song.getArtistName()
                                    : defaultValue(favorite.getArtistNameSnapshot(), "Unknown"))
                            .addedAt(favorite.getAddedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultValue(String value, String fallback) {
        String normalized = normalize(value);
        return normalized != null ? normalized : fallback;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getFavoriteCountsBySongIds(List<Long> songIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (songIds == null || songIds.isEmpty()) {
            return counts;
        }

        for (Object[] row : favoriteRepository.countBySongIds(songIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }

        return counts;
    }

}
