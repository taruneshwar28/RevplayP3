package com.revplay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    public UserProfileRequest() {
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getBio() {
        return bio;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserProfileRequest request;

        public Builder() {
            request = new UserProfileRequest();
        }

        public Builder username(String username) {
            request.username = username;
            return this;
        }

        public Builder displayName(String displayName) {
            request.displayName = displayName;
            return this;
        }

        public Builder bio(String bio) {
            request.bio = bio;
            return this;
        }

        public UserProfileRequest build() {
            return request;
        }
    }
}
