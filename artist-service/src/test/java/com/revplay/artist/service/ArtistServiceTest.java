package com.revplay.artist.service;

import com.revplay.artist.dto.ArtistProfileRequest;
import com.revplay.artist.dto.ArtistProfileResponse;
import com.revplay.artist.entity.Artist;
import com.revplay.artist.exception.ResourceNotFoundException;
import com.revplay.artist.repository.AlbumRepository;
import com.revplay.artist.repository.ArtistRepository;
import com.revplay.artist.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private ArtistService artistService;


    @Test
    void getArtistByIdThrowsWhenArtistMissing() {
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> artistService.getArtistById(99L)
        );

        assertEquals("Artist not found", exception.getMessage());
    }


    @Test
    void createArtistThrowsWhenProfileAlreadyExists() {
        ArtistProfileRequest request = ArtistProfileRequest.builder().stageName("Existing").build();
        when(artistRepository.existsByUserId(20L)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> artistService.createArtist(20L, request)
        );

        assertEquals("Artist profile already exists for this user", exception.getMessage());
        verify(artistRepository, never()).save(any(Artist.class));
    }

    @Test
    void updateArtistUpdatesOnlyProvidedFields() {
        Artist existingArtist = createArtist();
        ArtistProfileRequest request = ArtistProfileRequest.builder()
                .stageName("Updated Stage")
                .bio("Updated bio")
                .twitterUrl("https://twitter.com/updated")
                .profileImageUrl("https://cdn.example.com/updated.jpg")
                .build();

        when(artistRepository.findByUserId(10L)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.save(existingArtist)).thenReturn(existingArtist);
        mockCounts(existingArtist.getId(), 8L, 3L, 200L);

        ArtistProfileResponse response = artistService.updateArtist(10L, request);

        assertEquals("Updated Stage", existingArtist.getStageName());
        assertEquals("Updated bio", existingArtist.getBio());
        assertEquals("Rock", existingArtist.getGenre());
        assertEquals("https://instagram.com/artist", existingArtist.getInstagramUrl());
        assertEquals("https://twitter.com/updated", existingArtist.getTwitterUrl());
        assertEquals("https://youtube.com/artist", existingArtist.getYoutubeUrl());
        assertEquals("https://artist.example.com", existingArtist.getWebsiteUrl());
        assertEquals("https://cdn.example.com/updated.jpg", existingArtist.getProfileImageUrl());
        assertProfile(response, existingArtist, 8L, 3L, 200L);
    }

    @Test
    void updateArtistThrowsWhenArtistMissing() {
        when(artistRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> artistService.updateArtist(10L, ArtistProfileRequest.builder().stageName("Missing").build())
        );

        assertEquals("Artist profile not found", exception.getMessage());
        verify(artistRepository, never()).save(any(Artist.class));
    }

    @Test
    void getArtistByUserIdReturnsArtistEntity() {
        Artist artist = createArtist();
        when(artistRepository.findByUserId(10L)).thenReturn(Optional.of(artist));

        Artist result = artistService.getArtistByUserId(10L);

        assertSame(artist, result);
    }

    @Test
    void getArtistByUserIdThrowsWhenMissing() {
        when(artistRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> artistService.getArtistByUserId(10L)
        );

        assertEquals("Artist profile not found", exception.getMessage());
    }

    private void mockCounts(Long artistId, Long songCount, Long albumCount, Long totalPlays) {
        when(songRepository.countByArtistId(artistId)).thenReturn(songCount);
        when(albumRepository.countByArtistId(artistId)).thenReturn(albumCount);
        when(songRepository.getTotalPlaysByArtistId(artistId)).thenReturn(totalPlays);
    }

    private void assertProfile(ArtistProfileResponse response, Artist artist, Long songCount, Long albumCount, Long totalPlays) {
        assertNotNull(response);
        assertEquals(artist.getId(), response.getId());
        assertEquals(artist.getUserId(), response.getUserId());
        assertEquals(artist.getStageName(), response.getStageName());
        assertEquals(artist.getBio(), response.getBio());
        assertEquals(artist.getGenre(), response.getGenre());
        assertEquals(artist.getInstagramUrl(), response.getInstagramUrl());
        assertEquals(artist.getTwitterUrl(), response.getTwitterUrl());
        assertEquals(artist.getYoutubeUrl(), response.getYoutubeUrl());
        assertEquals(artist.getWebsiteUrl(), response.getWebsiteUrl());
        assertEquals(artist.getProfileImageUrl(), response.getProfileImageUrl());
        assertEquals(artist.getVerified(), response.getVerified());
        assertEquals(songCount, response.getSongCount());
        assertEquals(albumCount, response.getAlbumCount());
        assertEquals(totalPlays, response.getTotalPlays());
        assertEquals(artist.getCreatedAt(), response.getCreatedAt());
    }

    private Artist createArtist() {
        return Artist.builder()
                .id(1L)
                .userId(10L)
                .stageName("Original Stage")
                .bio("Original bio")
                .genre("Rock")
                .instagramUrl("https://instagram.com/artist")
                .twitterUrl("https://twitter.com/artist")
                .youtubeUrl("https://youtube.com/artist")
                .websiteUrl("https://artist.example.com")
                .profileImageUrl("https://cdn.example.com/artist.jpg")
                .verified(true)
                .createdAt(LocalDateTime.of(2025, 12, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2025, 12, 1, 9, 30))
                .build();
    }
}
