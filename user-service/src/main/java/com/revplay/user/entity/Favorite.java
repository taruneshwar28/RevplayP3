package com.revplay.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "songId"})
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long songId;

    private String songTitleSnapshot;

    private String artistNameSnapshot;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public Favorite() {
    }

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSongId() {
        return songId;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public String getSongTitleSnapshot() {
        return songTitleSnapshot;
    }

    public String getArtistNameSnapshot() {
        return artistNameSnapshot;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public void setSongTitleSnapshot(String songTitleSnapshot) {
        this.songTitleSnapshot = songTitleSnapshot;
    }

    public void setArtistNameSnapshot(String artistNameSnapshot) {
        this.artistNameSnapshot = artistNameSnapshot;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Favorite favorite;

        public Builder() {
            favorite = new Favorite();
        }

        public Builder id(Long id) {
            favorite.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            favorite.userId = userId;
            return this;
        }

        public Builder songId(Long songId) {
            favorite.songId = songId;
            return this;
        }

        public Builder addedAt(LocalDateTime addedAt) {
            favorite.addedAt = addedAt;
            return this;
        }

        public Builder songTitleSnapshot(String songTitleSnapshot) {
            favorite.songTitleSnapshot = songTitleSnapshot;
            return this;
        }

        public Builder artistNameSnapshot(String artistNameSnapshot) {
            favorite.artistNameSnapshot = artistNameSnapshot;
            return this;
        }

        public Favorite build() {
            return favorite;
        }
    }
}
