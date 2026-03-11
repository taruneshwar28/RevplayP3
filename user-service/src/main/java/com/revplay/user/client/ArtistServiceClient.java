package com.revplay.user.client;

import com.revplay.user.dto.SongDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "artist-service", configuration = com.revplay.user.config.FeignConfig.class)
public interface ArtistServiceClient {

    @GetMapping("/api/internal/songs/{songId}")
    SongDto getSongById(@PathVariable("songId") Long songId);

    @PostMapping("/api/internal/songs/batch")
    List<SongDto> getSongsByIds(@RequestBody List<Long> ids);
}
