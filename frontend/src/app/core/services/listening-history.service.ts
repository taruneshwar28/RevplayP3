import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay, tap } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface ListeningHistoryItem {
  id: number;
  songId: number;
  songTitle: string;
  artistName: string;
  albumTitle: string;
  coverImageUrl?: string;
  playedAt: string;
  listenedDuration: number;
}

export interface PlayResponse {
  songId: number;
  title: string;
  artistName: string;
  albumTitle: string;
  fileUrl: string;
  coverImageUrl?: string;
  duration: number;
}

interface HistoryPage {
  content: ListeningHistoryItem[];
}

@Injectable({
  providedIn: 'root',
})
export class ListeningHistoryService {
  private readonly baseUrl = environment.apiUrl;
  private readonly historyCache = new Map<string, Observable<ListeningHistoryItem[]>>();

  constructor(private readonly http: HttpClient) {}

  recordPlay(songId: number): Observable<PlayResponse> {
    return this.http.post<PlayResponse>(`${this.baseUrl}/player/play`, { songId }).pipe(
      tap(() => this.historyCache.clear())
    );
  }

  getRecentHistory(page = 0, size = 20): Observable<ListeningHistoryItem[]> {
    const cacheKey = `${page}:${size}`;
    const cached = this.historyCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const request$ = this.http
      .get<HistoryPage>(`${this.baseUrl}/history/recent`, {
        params: { page, size },
      })
      .pipe(
        map((response) => response.content ?? []),
        shareReplay(1)
      );

    this.historyCache.set(cacheKey, request$);
    return request$;
  }

  clearHistory(): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/history`).pipe(
      tap(() => this.historyCache.clear())
    );
  }
}
