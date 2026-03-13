import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export type SongVisibility = 'PUBLIC' | 'PRIVATE' | 'UNLISTED';

export interface Song {
  id: number;
  title: string;
  url: string;
}

export interface SongRequest {
  title: string;
  duration: number;
  genre?: string;
  fileUrl: string;
  coverImageUrl?: string;
  visibility: SongVisibility;
  albumId?: number;
}

export interface SongResponse {
  id: number;
  title: string;
  artistId: number;
  artistName: string;
  albumId?: number;
  albumTitle?: string;
  duration: number;
  genre?: string;
  fileUrl: string;
  coverImageUrl?: string;
  visibility: SongVisibility;
  playCount: number;
  createdAt: string;
}

export interface SongFilters {
  title?: string;
  genre?: string;
  album?: string;
  releaseYear?: string;
  sort?: string;
}

@Injectable({
  providedIn: 'root',
})
export class SongService {
  private readonly artistSongsUrl = `${environment.apiUrl}/artists/songs`;
  private readonly musicUrl = `${environment.apiUrl}/music`;

  constructor(private readonly http: HttpClient) {}

  uploadSong(data: SongRequest): Observable<SongResponse> {
    return this.http.post<SongResponse>(this.artistSongsUrl, data);
  }

  getSongs(): Observable<SongResponse[]>;
  getSongs(page: number, size: number, filters?: SongFilters): Observable<any>;
  getSongs(page?: number, size?: number, filters?: SongFilters): Observable<SongResponse[] | any> {
    if (typeof page === 'number' && typeof size === 'number') {
      let params = new HttpParams()
        .set('page', page)
        .set('size', size);

      if (filters?.title) params = params.set('title', filters.title);
      if (filters?.genre) params = params.set('genre', filters.genre);
      if (filters?.album) params = params.set('album', filters.album);
      if (filters?.releaseYear) params = params.set('releaseYear', filters.releaseYear);
      if (filters?.sort) params = params.set('sort', filters.sort);

      return this.http.get(`${this.musicUrl}/songs`, { params });
    }

    return this.http.get<SongResponse[]>(this.artistSongsUrl);
  }

  getAllSongs(): Observable<Song[]> {
    return this.http.get<Song[]>(`${this.musicUrl}/songs/all`);
  }

  search(keyword: string): Observable<any> {
    return this.http.get(`${this.musicUrl}/search`, {
      params: { keyword },
    });
  }

  getSong(songId: number): Observable<SongResponse> {
    return this.http.get<SongResponse>(`${this.artistSongsUrl}/${songId}`);
  }

  getSongById(songId: number): Observable<any> {
    return this.http.get(`${this.musicUrl}/songs/${songId}`);
  }

  getArtistById(artistId: number): Observable<any> {
    return this.http.get(`${this.musicUrl}/artists/${artistId}`);
  }

  getAlbumById(albumId: number): Observable<any> {
    return this.http.get(`${this.musicUrl}/albums/${albumId}`);
  }

  updateSong(songId: number, data: Partial<SongRequest>): Observable<SongResponse> {
    return this.http.put<SongResponse>(`${this.artistSongsUrl}/${songId}`, data);
  }

  deleteSong(songId: number): Observable<void> {
    return this.http.delete<void>(`${this.artistSongsUrl}/${songId}`);
  }

  updateVisibility(songId: number, visibility: SongVisibility): Observable<SongResponse> {
    return this.http.put<SongResponse>(`${this.artistSongsUrl}/${songId}/visibility`, {}, {
      params: { visibility },
    });
  }
}
