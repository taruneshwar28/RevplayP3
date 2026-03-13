package com.revplay.artist.service;

import com.revplay.artist.dto.SongResponse;
import com.revplay.artist.dto.SongUpdateRequest;
import com.revplay.artist.dto.SongUploadRequest;
import com.revplay.artist.entity.Album;
import com.revplay.artist.entity.Artist;
import com.revplay.artist.entity.Song;
import com.revplay.artist.entity.Visibility;
import com.revplay.artist.exception.ResourceNotFoundException;
import com.revplay.artist.exception.UnauthorizedException;
import com.revplay.artist.repository.AlbumRepository;
import com.revplay.artist.repository.ArtistRepository;
import com.revplay.artist.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SongService {

    private static final Logger log = LoggerFactory.getLogger(SongService.class);

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;

    public SongService(SongRepository songRepository,
                      ArtistRepository artistRepository,
                      AlbumRepository albumRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
    }

    @Transactional
    public SongResponse uploadSong(Long userId, SongUploadRequest request) {
        log.info("Uploading song for userId={} title={}", userId, request.getTitle());
        Artist artist = artistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.warn("Artist profile not found for userId={}; creating default artist profile during song upload", userId);
                    return artistRepository.save(Artist.builder()
                            .userId(userId)
                            .stageName("Artist")
                            .bio("")
                            .genre("")
                            .instagramUrl("")
                            .twitterUrl("")
                            .youtubeUrl("")
                            .websiteUrl("")
                            .profileImageUrl("")
                            .verified(false)
                            .build());
                });

        // Validate album ownership if albumId is provided
        if (request.getAlbumId() != null) {
            albumRepository.findByArtistIdAndId(artist.getId(), request.getAlbumId())
                    .orElseThrow(() -> {
                        log.warn("Album validation failed during song upload for userId={} artistId={} albumId={}", userId, artist.getId(), request.getAlbumId());
                        return new ResourceNotFoundException("Album not found or does not belong to this artist");
                    });
        }

        Song song = Song.builder()
                .artistId(artist.getId())
                .albumId(request.getAlbumId())
                .title(request.getTitle())
                .duration(request.getDuration())
                .genre(request.getGenre())
                .fileUrl(request.getFileUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .visibility(request.getVisibility())
                .playCount(0L)
                .build();

        song = songRepository.save(song);
        log.info("Uploaded song songId={} for artistId={}", song.getId(), artist.getId());
        return buildSongResponse(song, artist);
    }

    @Transactional(readOnly = true)
    public List<SongResponse> getSongsByArtist(Long userId) {
        log.info("Fetching songs for userId={}", userId);
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cannot fetch songs because artist profile was not found for userId={}", userId);
                    return new ResourceNotFoundException("Artist profile not found");
                });

        return getSongsByArtistId(artist.getId());
    }

    @Transactional(readOnly = true)
    public List<SongResponse> getSongsByArtistId(Long artistId) {
        log.info("Fetching songs for artistId={}", artistId);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> {
                    log.warn("Artist not found while fetching songs for artistId={}", artistId);
                    return new ResourceNotFoundException("Artist not found");
                });

        List<Song> songs = songRepository.findByArtistId(artist.getId());
        return songs.stream()
                .map(song -> buildSongResponse(song, artist))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SongResponse getSong(Long songId) {
        log.info("Fetching song songId={}", songId);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> {
                    log.warn("Song not found for songId={}", songId);
                    return new ResourceNotFoundException("Song not found");
                });

        Artist artist = artistRepository.findById(song.getArtistId())
                .orElseThrow(() -> {
                    log.warn("Artist not found for songId={} artistId={}", songId, song.getArtistId());
                    return new ResourceNotFoundException("Artist not found");
                });

        return buildSongResponse(song, artist);
    }

    @Transactional
    public SongResponse updateSong(Long userId, Long songId, SongUpdateRequest request) {
        log.info("Updating song songId={} for userId={}", songId, userId);
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cannot update song songId={} because artist profile was not found for userId={}", songId, userId);
                    return new ResourceNotFoundException("Artist profile not found");
                });

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> {
                    log.warn("Cannot update song because songId={} was not found", songId);
                    return new ResourceNotFoundException("Song not found");
                });

        if (!song.getArtistId().equals(artist.getId())) {
            log.warn("Unauthorized song update attempt for songId={} by userId={} artistId={}", songId, userId, artist.getId());
            throw new UnauthorizedException("You are not authorized to update this song");
        }

        // albumId = 0 is treated as removing the song from its album
        if (request.getAlbumId() != null && request.getAlbumId() != 0L) {
            albumRepository.findByArtistIdAndId(artist.getId(), request.getAlbumId())
                    .orElseThrow(() -> {
                        log.warn("Album validation failed during song update for songId={} artistId={} albumId={}", songId, artist.getId(), request.getAlbumId());
                        return new ResourceNotFoundException("Album not found or does not belong to this artist");
                    });
        }

        if (request.getTitle() != null) {
            song.setTitle(request.getTitle());
        }
        if (request.getGenre() != null) {
            song.setGenre(request.getGenre());
        }
        if (request.getDuration() != null) {
            song.setDuration(request.getDuration());
        }
        if (request.getVisibility() != null) {
            song.setVisibility(request.getVisibility());
        }
        if (request.getAlbumId() != null) {
            song.setAlbumId(request.getAlbumId() == 0L ? null : request.getAlbumId());
        }

        song = songRepository.save(song);
        log.info("Updated song songId={} for artistId={}", song.getId(), artist.getId());
        return buildSongResponse(song, artist);
    }

    @Transactional
    public void deleteSong(Long userId, Long songId) {
        log.info("Deleting song songId={} for userId={}", songId, userId);
        Artist artist = artistRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cannot delete song songId={} because artist profile was not found for userId={}", songId, userId);
                    return new ResourceNotFoundException("Artist profile not found");
                });

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> {
                    log.warn("Cannot delete song because songId={} was not found", songId);
                    return new ResourceNotFoundException("Song not found");
                });

        if (!song.getArtistId().equals(artist.getId())) {
            log.warn("Unauthorized song delete attempt for songId={} by userId={} artistId={}", songId, userId, artist.getId());
            throw new UnauthorizedException("You are not authorized to delete this song");
        }

        songRepository.delete(song);
        log.info("Deleted song songId={} for artistId={}", songId, artist.getId());
    }

    @Transactional(readOnly = true)
    public Page<SongResponse> getPublicSongs(Pageable pageable) {
        log.info("Fetching public songs page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Song> songs = songRepository.findByVisibility(Visibility.PUBLIC, pageable);
        return songs.map(song -> {
            Artist artist = artistRepository.findById(song.getArtistId())
                    .orElse(null);
            return buildSongResponse(song, artist);
        });
    }

    @Transactional
    public void incrementPlayCount(Long songId) {
        log.info("Incrementing play count for songId={}", songId);
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> {
                    log.warn("Cannot increment play count because songId={} was not found", songId);
                    return new ResourceNotFoundException("Song not found");
                });

        songRepository.incrementPlayCount(songId);
        log.info("Incremented play count for songId={} currentPlayCount={}", songId, song.getPlayCount());
    }

    @Transactional(readOnly = true)
    public List<SongResponse> getSongsByIds(List<Long> ids) {
        log.info("Fetching songs by ids count={}", ids.size());
        List<Song> songs = songRepository.findByIdIn(ids);
        return songs.stream()
                .map(song -> {
                    Artist artist = artistRepository.findById(song.getArtistId())
                            .orElse(null);
                    return buildSongResponse(song, artist);
                })
                .collect(Collectors.toList());
    }

    private SongResponse buildSongResponse(Song song, Artist artist) {
        String artistName = artist != null ? artist.getStageName() : null;
        String albumTitle = null;

        if (song.getAlbumId() != null) {
            Album album = albumRepository.findById(song.getAlbumId()).orElse(null);
            if (album != null) {
                albumTitle = album.getTitle();
            }
        }

        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artistId(song.getArtistId())
                .artistName(artistName)
                .albumId(song.getAlbumId())
                .albumTitle(albumTitle)
                .duration(song.getDuration())
                .genre(song.getGenre())
                .fileUrl(song.getFileUrl())
                .coverImageUrl(song.getCoverImageUrl())
                .visibility(song.getVisibility())
                .playCount(song.getPlayCount())
                .createdAt(song.getCreatedAt())
                .build();
    }
}
