import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { FavoriteResponse, FavoriteService } from 'src/app/core/services/favorite.service';
import {
  SongCatalogResponse,
  SongLibraryService,
} from 'src/app/core/services/song-library.service';

@Component({
  selector: 'app-browse',
  templateUrl: './browse.component.html',
  styleUrls: ['./browse.component.css'],
})
export class BrowseComponent implements OnInit {
  songs: Array<SongCatalogResponse & { isFavorite: boolean }> = [];
  filteredSongs: Array<SongCatalogResponse & { isFavorite: boolean }> = [];
  currentPage = 0;
  readonly pageSize = 5;
  totalPages = 1;
  searchQuery = '';
  artistFilter = '';
  albumFilter = '';
  genreFilter = '';
  dateFilter = '';
  sortOption = 'none';
  favoriteBusySongIds = new Set<number>();

  loading = false;
  errorMessage = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly songLibraryService: SongLibraryService,
    private readonly favoriteService: FavoriteService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.searchQuery = (params.get('q') ?? '').trim();
      this.artistFilter = (params.get('artist') ?? '').trim();
      this.albumFilter = (params.get('album') ?? '').trim();
      this.genreFilter = (params.get('genre') ?? '').trim();
      this.dateFilter = (params.get('date') ?? '').trim();
      this.sortOption = (params.get('sort') ?? 'none').trim() || 'none';
      const pageParam = Number(params.get('page') ?? '1');
      this.currentPage = Number.isFinite(pageParam) && pageParam > 0 ? pageParam - 1 : 0;
      this.loadSongs();
    });
  }

  loadSongs(): void {
    this.loading = true;
    this.errorMessage = '';
    const hasClientFilters =
      !!this.artistFilter ||
      !!this.albumFilter ||
      !!this.genreFilter ||
      !!this.dateFilter ||
      this.sortOption !== 'none';

    if (this.searchQuery) {
      const searchRequest$ = hasClientFilters
        ? this.songLibraryService.searchSongs(this.searchQuery, 0, 500)
        : this.songLibraryService.searchSongs(this.searchQuery, this.currentPage, this.pageSize);

      searchRequest$.subscribe({
        next: (search) => {
          if (hasClientFilters) {
            this.updateSongs(search.songs, true);
            this.loadFavoritesInBackground();
            return;
          }

          this.totalPages = Math.max(1, Math.ceil((search.totalResults ?? search.songs.length) / this.pageSize));
          this.updateSongs(search.songs, false);
          this.loadFavoritesInBackground();
        },
        error: () => this.handleLoadError(),
      });
      return;
    }

    const songsRequest$ = hasClientFilters
      ? this.songLibraryService.getPublicSongs(0, 500)
      : this.songLibraryService.getPublicSongs(this.currentPage, this.pageSize);

    songsRequest$.subscribe({
      next: (songs) => {
        if (hasClientFilters) {
          this.updateSongs(songs.content, true);
          this.loadFavoritesInBackground();
          return;
        }

        this.totalPages = songs.totalPages ?? 1;
        this.updateSongs(songs.content, false);
        this.loadFavoritesInBackground();
      },
      error: () => this.handleLoadError(),
    });
  }

  toggleFavorite(songId: number, isFavorite: boolean): void {
    if (this.loading || this.favoriteBusySongIds.has(songId)) {
      return;
    }

    this.favoriteBusySongIds.add(songId);
    const currentSong = this.songs.find((song) => song.id === songId);
    this.updateFavoriteState(songId, !isFavorite);

    const request$ = isFavorite
      ? this.favoriteService.remove(songId)
      : this.favoriteService.add(songId, currentSong?.title, currentSong?.artistName);

    request$.subscribe({
      next: () => {
        this.favoriteBusySongIds.delete(songId);
      },
      error: () => {
        this.updateFavoriteState(songId, isFavorite);
        this.favoriteBusySongIds.delete(songId);
      },
    });
  }

  viewSongDetails(songId: number): void {
    this.router.navigate(['/browse', songId], {
      queryParamsHandling: 'preserve',
    });
  }

  playSong(songId: number): void {
    this.router.navigate(['/player'], {
      queryParams: { songId },
    });
  }

  previousPage(): void {
    if (this.currentPage <= 0) {
      return;
    }

    this.navigateToPage(this.currentPage);
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages - 1) {
      return;
    }

    this.navigateToPage(this.currentPage + 2);
  }

  get hasSongs(): boolean {
    return this.filteredSongs.length > 0;
  }

  private updateSongs(rawSongs: SongCatalogResponse[], applyClientFiltering: boolean): void {
    const favoriteIds = this.favoriteService.getCachedFavoriteSongIds();
    this.songs = rawSongs.map((song) => ({
      ...song,
      isFavorite: favoriteIds.has(song.id),
    }));

    if (applyClientFiltering) {
      this.applyFiltersAndPagination();
    } else {
      this.filteredSongs = [...this.songs];
    }

    this.loading = false;
  }

  private loadFavoritesInBackground(): void {
    this.favoriteService.get().pipe(catchError(() => of([]))).subscribe((favorites) => {
      const ids = new Set((favorites as FavoriteResponse[]).map((row) => row.songId));
      this.songs = this.songs.map((song) => ({ ...song, isFavorite: ids.has(song.id) }));
      this.filteredSongs = this.filteredSongs.map((song) => ({ ...song, isFavorite: ids.has(song.id) }));
    });
  }

  private updateFavoriteState(songId: number, isFavorite: boolean): void {
    this.songs = this.songs.map((song) =>
      song.id === songId ? { ...song, isFavorite } : song
    );
    this.filteredSongs = this.filteredSongs.map((song) =>
      song.id === songId ? { ...song, isFavorite } : song
    );
  }

  private handleLoadError(): void {
    this.songs = [];
    this.totalPages = 1;
    this.loading = false;
  }

  private navigateToPage(page: number): void {
    this.router.navigate(['/browse'], {
      queryParams: {
        q: this.searchQuery || undefined,
        artist: this.artistFilter || undefined,
        album: this.albumFilter || undefined,
        genre: this.genreFilter || undefined,
        date: this.dateFilter || undefined,
        sort: this.sortOption !== 'none' ? this.sortOption : undefined,
        page,
      },
    });
  }

  private applyFiltersAndPagination(): void {
    let result = [...this.songs];
    const hasFilters =
      !!this.searchQuery ||
      !!this.artistFilter ||
      !!this.albumFilter ||
      !!this.genreFilter ||
      !!this.dateFilter ||
      this.sortOption !== 'none';

    if (this.artistFilter) {
      const needle = this.artistFilter.toLowerCase();
      result = result.filter((song) => (song.artistName ?? '').toLowerCase().includes(needle));
    }

    if (this.albumFilter) {
      const needle = this.albumFilter.toLowerCase();
      result = result.filter((song) => (song.albumTitle ?? '').toLowerCase().includes(needle));
    }

    if (this.genreFilter) {
      const needle = this.genreFilter.toLowerCase();
      result = result.filter((song) => (song.genre ?? '').toLowerCase().includes(needle));
    }

    if (this.dateFilter) {
      result = result.filter((song) => (song.createdAt ?? '').slice(0, 10) === this.dateFilter);
    }

    switch (this.sortOption) {
      case 'titleAsc':
        result.sort((a, b) => a.title.localeCompare(b.title));
        break;
      case 'titleDesc':
        result.sort((a, b) => b.title.localeCompare(a.title));
        break;
      case 'artistAsc':
        result.sort((a, b) => (a.artistName ?? '').localeCompare(b.artistName ?? ''));
        break;
      case 'playsDesc':
        result.sort((a, b) => (b.playCount ?? 0) - (a.playCount ?? 0));
        break;
      case 'newest':
        result.sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
        break;
      case 'oldest':
        result.sort((a, b) => (a.createdAt ?? '').localeCompare(b.createdAt ?? ''));
        break;
      default:
        break;
    }

    if (!hasFilters) {
      result = [...this.songs];
    }

    this.totalPages = Math.max(1, Math.ceil(result.length / this.pageSize));
    if (this.currentPage > this.totalPages - 1) {
      this.currentPage = this.totalPages - 1;
    }

    const start = this.currentPage * this.pageSize;
    this.filteredSongs = result.slice(start, start + this.pageSize);
  }

  get visibleSongs(): Array<SongCatalogResponse & { isFavorite: boolean }> {
    return this.filteredSongs;
  }
}
