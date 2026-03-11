package com.revplay.artist.service;

import com.revplay.artist.dto.ArtistProfileRequest;
import com.revplay.artist.dto.ArtistProfileResponse;
import com.revplay.artist.entity.Artist;
import com.revplay.artist.exception.ResourceNotFoundException;
import com.revplay.artist.repository.AlbumRepository;
import com.revplay.artist.repository.ArtistRepository;
import com.revplay.artist.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;

    public ArtistService(ArtistRepository artistRepository,
                        SongRepository songRepository,
                        AlbumRepository albumRepository) {
        this.artistRepository = artistRepository;
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
    }

    @Transactional(readOnly = true)
    public ArtistProfileResponse getArtistProfile(Long userId) {
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found"));

        return buildArtistProfileResponse(artist);
    }

    @Transactional(readOnly = true)
    public ArtistProfileResponse getArtistById(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));

        return buildArtistProfileResponse(artist);
    }

    @Transactional
    public ArtistProfileResponse createArtist(Long userId, ArtistProfileRequest request) {
        if (artistRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Artist profile already exists for this user");
        }

        Artist artist = Artist.builder()
                .userId(userId)
                .stageName(request.getStageName())
                .bio(request.getBio())
                .genre(request.getGenre())
                .instagramUrl(request.getInstagramUrl())
                .twitterUrl(request.getTwitterUrl())
                .youtubeUrl(request.getYoutubeUrl())
                .websiteUrl(request.getWebsiteUrl())
                .profileImageUrl(request.getProfileImageUrl())
                .verified(false)
                .build();

        artist = artistRepository.save(artist);
        return buildArtistProfileResponse(artist);
    }

    @Transactional
    public ArtistProfileResponse updateArtist(Long userId, ArtistProfileRequest request) {
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found"));

        if (request.getStageName() != null) {
            artist.setStageName(request.getStageName());
        }
        if (request.getBio() != null) {
            artist.setBio(request.getBio());
        }
        if (request.getGenre() != null) {
            artist.setGenre(request.getGenre());
        }
        if (request.getInstagramUrl() != null) {
            artist.setInstagramUrl(request.getInstagramUrl());
        }
        if (request.getTwitterUrl() != null) {
            artist.setTwitterUrl(request.getTwitterUrl());
        }
        if (request.getYoutubeUrl() != null) {
            artist.setYoutubeUrl(request.getYoutubeUrl());
        }
        if (request.getWebsiteUrl() != null) {
            artist.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getProfileImageUrl() != null) {
            artist.setProfileImageUrl(request.getProfileImageUrl());
        }

        artist = artistRepository.save(artist);
        return buildArtistProfileResponse(artist);
    }

    @Transactional(readOnly = true)
    public Artist getArtistByUserId(Long userId) {
        return artistRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found"));
    }

    private ArtistProfileResponse buildArtistProfileResponse(Artist artist) {
        Long songCount = songRepository.countByArtistId(artist.getId());
        Long albumCount = albumRepository.countByArtistId(artist.getId());
        Long totalPlays = songRepository.getTotalPlaysByArtistId(artist.getId());

        if (totalPlays == null) {
            totalPlays = 0L;
        }

        return ArtistProfileResponse.builder()
                .id(artist.getId())
                .userId(artist.getUserId())
                .stageName(artist.getStageName())
                .bio(artist.getBio())
                .genre(artist.getGenre())
                .instagramUrl(artist.getInstagramUrl())
                .twitterUrl(artist.getTwitterUrl())
                .youtubeUrl(artist.getYoutubeUrl())
                .websiteUrl(artist.getWebsiteUrl())
                .profileImageUrl(artist.getProfileImageUrl())
                .verified(artist.getVerified())
                .songCount(songCount)
                .albumCount(albumCount)
                .totalPlays(totalPlays)
                .createdAt(artist.getCreatedAt())
                .build();
    }
}
