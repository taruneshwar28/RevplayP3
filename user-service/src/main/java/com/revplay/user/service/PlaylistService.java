package com.revplay.user.service;

import com.revplay.user.client.ArtistServiceClient;
import com.revplay.user.dto.PlaylistRequest;
import com.revplay.user.dto.PlaylistResponse;
import com.revplay.user.dto.PlaylistSongRequest;
import com.revplay.user.dto.SongDto;
import com.revplay.user.entity.Playlist;
import com.revplay.user.entity.PlaylistSong;
import com.revplay.user.exception.ResourceNotFoundException;
import com.revplay.user.repository.PlaylistRepository;
import com.revplay.user.repository.PlaylistSongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final ArtistServiceClient artistServiceClient;

    public PlaylistService(PlaylistRepository playlistRepository,
                          PlaylistSongRepository playlistSongRepository,
                          ArtistServiceClient artistServiceClient) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.artistServiceClient = artistServiceClient;
    }

    @Transactional
    public PlaylistResponse createPlaylist(Long userId, PlaylistRequest request) {
        Playlist playlist = Playlist.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();

        Playlist savedPlaylist = playlistRepository.save(playlist);
        return mapToResponse(savedPlaylist, new ArrayList<>(), 0);
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getPlaylists(Long userId) {
        List<Playlist> playlists = playlistRepository.findByUserId(userId);
        return playlists.stream()
                .map(playlist -> {
                    Long songCount = playlistSongRepository.countByPlaylistId(playlist.getId());
                    return mapToResponse(playlist, new ArrayList<>(), songCount.intValue());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByUserIdAndId(userId, playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylistIdOrderByPosition(playlistId);

        List<SongDto> songs = new ArrayList<>();
        if (!playlistSongs.isEmpty()) {
            List<Long> songIds = playlistSongs.stream()
                    .map(PlaylistSong::getSongId)
                    .collect(Collectors.toList());

            try {
                songs = artistServiceClient.getSongsByIds(songIds);
            } catch (Exception e) {
                // If artist service is down, return playlist without song details
                System.err.println("Failed to fetch song details: " + e.getMessage());
            }
        }

        return mapToResponse(playlist, songs, playlistSongs.size());
    }

    @Transactional
    public PlaylistResponse updatePlaylist(Long userId, Long playlistId, PlaylistRequest request) {
        Playlist playlist = playlistRepository.findByUserIdAndId(userId, playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            playlist.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            playlist.setDescription(request.getDescription().trim());
        }
        if (request.getIsPublic() != null) {
            playlist.setIsPublic(request.getIsPublic());
        }

        Playlist updatedPlaylist = playlistRepository.save(playlist);
        int songCount = playlistSongRepository.countByPlaylistId(updatedPlaylist.getId()).intValue();
        return mapToResponse(updatedPlaylist, new ArrayList<>(), songCount);
    }

    @Transactional
    public void deletePlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistRepository.findByUserIdAndId(userId, playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        playlistSongRepository.deleteByPlaylistId(playlistId);
        playlistRepository.delete(playlist);
    }

    @Transactional
    public void addSong(Long userId, Long playlistId, PlaylistSongRequest request) {
        playlistRepository.findByUserIdAndId(userId, playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        boolean alreadyExists = playlistSongRepository.findByPlaylistIdOrderByPosition(playlistId).stream()
                .anyMatch(song -> song.getSongId().equals(request.getSongId()));
        if (alreadyExists) {
            return;
        }

        Integer position = request.getPosition();
        if (position == null) {
            Integer maxPosition = playlistSongRepository.findMaxPositionByPlaylistId(playlistId);
            position = (maxPosition != null ? maxPosition : 0) + 1;
        }

        PlaylistSong playlistSong = PlaylistSong.builder()
                .playlistId(playlistId)
                .songId(request.getSongId())
                .position(position)
                .build();

        playlistSongRepository.save(playlistSong);
    }

    @Transactional
    public void removeSong(Long userId, Long playlistId, Long songId) {
        playlistRepository.findByUserIdAndId(userId, playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    private PlaylistResponse mapToResponse(Playlist playlist, List<SongDto> songs, int songCount) {
        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .isPublic(playlist.getIsPublic())
                .songCount(songCount)
                .songs(songs)
                .createdAt(playlist.getCreatedAt())
                .build();
    }
}
