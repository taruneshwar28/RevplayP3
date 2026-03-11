import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'frontend';
  searchTerm = '';
  artistFilter = '';
  albumFilter = '';
  genreFilter = '';
  dateFilter = '';
  sortOption = 'none';

  constructor(
    public authService: AuthService,
    public router: Router,
  ) {
    this.syncSearchFromUrl();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.syncSearchFromUrl());
  }

  onSearchFocus(): void {
    if (!this.router.url.startsWith('/browse')) {
      this.router.navigate(['/browse'], {
        queryParams: {
          q: this.searchTerm.trim() || undefined,
        },
      });
    }
  }

  onSearch(): void {
    this.navigateToBrowseWithFilters(1);
  }

  applyFilters(): void {
    this.navigateToBrowseWithFilters(1);
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.artistFilter = '';
    this.albumFilter = '';
    this.genreFilter = '';
    this.dateFilter = '';
    this.sortOption = 'none';
    this.navigateToBrowseWithFilters(1);
  }

  private syncSearchFromUrl(): void {
    if (!this.router.url.startsWith('/browse')) {
      this.searchTerm = '';
      this.artistFilter = '';
      this.albumFilter = '';
      this.genreFilter = '';
      this.dateFilter = '';
      this.sortOption = 'none';
      return;
    }

    const parsedUrl = this.router.parseUrl(this.router.url);
    const { q, artist, album, genre, date, sort } = parsedUrl.queryParams;

    this.searchTerm = typeof q === 'string' ? q : '';
    this.artistFilter = typeof artist === 'string' ? artist : '';
    this.albumFilter = typeof album === 'string' ? album : '';
    this.genreFilter = typeof genre === 'string' ? genre : '';
    this.dateFilter = typeof date === 'string' ? date : '';
    this.sortOption = typeof sort === 'string' && sort ? sort : 'none';
  }

  private navigateToBrowseWithFilters(page: number): void {
    const q = this.searchTerm.trim();
    const artist = this.artistFilter.trim();
    const album = this.albumFilter.trim();
    const genre = this.genreFilter.trim();
    const date = this.dateFilter;
    const sort = this.sortOption !== 'none' ? this.sortOption : undefined;

    this.router.navigate(['/browse'], {
      queryParams: {
        q: q || undefined,
        artist: artist || undefined,
        album: album || undefined,
        genre: genre || undefined,
        date: date || undefined,
        sort,
        page,
      },
    });
  }
}
