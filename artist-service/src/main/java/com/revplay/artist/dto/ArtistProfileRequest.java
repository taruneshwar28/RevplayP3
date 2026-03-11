package com.revplay.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArtistProfileRequest {

    @NotBlank(message = "Stage name is required")
    @Size(min = 2, max = 100, message = "Stage name must be between 2 and 100 characters")
    private String stageName;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 100, message = "Genre must not exceed 100 characters")
    private String genre;

    private String instagramUrl;

    private String twitterUrl;

    private String youtubeUrl;

    private String websiteUrl;

    private String profileImageUrl;

    // Constructors
    public ArtistProfileRequest() {
    }

    // Getters and Setters
    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ArtistProfileRequest request = new ArtistProfileRequest();

        public Builder stageName(String stageName) {
            request.stageName = stageName;
            return this;
        }

        public Builder bio(String bio) {
            request.bio = bio;
            return this;
        }

        public Builder genre(String genre) {
            request.genre = genre;
            return this;
        }

        public Builder instagramUrl(String instagramUrl) {
            request.instagramUrl = instagramUrl;
            return this;
        }

        public Builder twitterUrl(String twitterUrl) {
            request.twitterUrl = twitterUrl;
            return this;
        }

        public Builder youtubeUrl(String youtubeUrl) {
            request.youtubeUrl = youtubeUrl;
            return this;
        }

        public Builder websiteUrl(String websiteUrl) {
            request.websiteUrl = websiteUrl;
            return this;
        }

        public Builder profileImageUrl(String profileImageUrl) {
            request.profileImageUrl = profileImageUrl;
            return this;
        }

        public ArtistProfileRequest build() {
            return request;
        }
    }
}
