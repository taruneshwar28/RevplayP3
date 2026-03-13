import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FavoriteResponse {
  id: number;
  songId: number;
  songTitle: string;
  artistName: string;
  addedAt: string;
}

@Injectable({ providedIn: 'root' })
export class FavoriteService {
  private readonly baseUrl = `${environment.apiUrl}/favorites`;
  private readonly storageKey = 'revplay.favorite-cache';
  private favoritesCache$?: Observable<FavoriteResponse[]>;
  private favoritesSnapshot: FavoriteResponse[] = [];

  constructor(private readonly http: HttpClient) {
    this.favoritesSnapshot = this.readStoredFavorites();
  }

  add(songId: number, songTitle?: string, artistName?: string): Observable<void> {
    this.upsertFavoriteSnapshot(songId, songTitle, artistName);
    return this.http.post<void>(`${this.baseUrl}/${songId}`, {
      songTitle,
      artistName,
    }).pipe(tap(() => this.invalidateFavoritesCache()));
  }

  remove(songId: number): Observable<void> {
    const previous = [...this.favoritesSnapshot];
    this.setFavoritesSnapshot(this.favoritesSnapshot.filter((favorite) => favorite.songId !== songId));
    return this.http.delete<void>(`${this.baseUrl}/${songId}`).pipe(tap(() => this.invalidateFavoritesCache()));
  }

  get(): Observable<FavoriteResponse[]> {
    if (!this.favoritesCache$ && this.favoritesSnapshot.length > 0) {
      return of(this.favoritesSnapshot);
    }

    if (!this.favoritesCache$) {
      this.favoritesCache$ = this.http.get<FavoriteResponse[]>(this.baseUrl).pipe(
        tap((favorites) => this.setFavoritesSnapshot(favorites)),
        shareReplay(1)
      );
    }

    return this.favoritesCache$;
  }

  refresh(): Observable<FavoriteResponse[]> {
    this.favoritesCache$ = this.http.get<FavoriteResponse[]>(this.baseUrl).pipe(
      tap((favorites) => this.setFavoritesSnapshot(favorites)),
      shareReplay(1)
    );

    return this.favoritesCache$;
  }

  getCachedFavorites(): FavoriteResponse[] {
    return [...this.favoritesSnapshot];
  }

  getCachedFavoriteSongIds(): Set<number> {
    return new Set(this.favoritesSnapshot.map((favorite) => favorite.songId));
  }

  getCounts(songIds: number[]): Observable<Record<number, number>> {
    return this.http.get<Record<number, number>>(`${this.baseUrl}/counts`, {
      params: {
        ids: songIds,
      },
    });
  }

  private invalidateFavoritesCache(): void {
    this.favoritesCache$ = undefined;
  }

  private setFavoritesSnapshot(favorites: FavoriteResponse[]): void {
    this.favoritesSnapshot = [...favorites];
    localStorage.setItem(this.storageKey, JSON.stringify(this.favoritesSnapshot));
  }

  private upsertFavoriteSnapshot(songId: number, songTitle?: string, artistName?: string): void {
    const existing = this.favoritesSnapshot.find((favorite) => favorite.songId === songId);
    if (existing) {
      return;
    }

    this.setFavoritesSnapshot([
      {
        id: Date.now(),
        songId,
        songTitle: songTitle?.trim() || 'Unknown',
        artistName: artistName?.trim() || 'Unknown',
        addedAt: new Date().toISOString(),
      },
      ...this.favoritesSnapshot,
    ]);
  }

  private readStoredFavorites(): FavoriteResponse[] {
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) {
        return [];
      }

      const parsed = JSON.parse(raw) as FavoriteResponse[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
