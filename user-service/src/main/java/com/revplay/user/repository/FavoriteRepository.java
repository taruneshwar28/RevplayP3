package com.revplay.user.repository;

import com.revplay.user.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndSongId(Long userId, Long songId);

    boolean existsByUserIdAndSongId(Long userId, Long songId);

    Long countByUserId(Long userId);

    @Query("SELECT f.songId, COUNT(f) FROM Favorite f WHERE f.songId IN :songIds GROUP BY f.songId")
    List<Object[]> countBySongIds(@Param("songIds") List<Long> songIds);
}
