import { Component, OnInit } from '@angular/core';
import { FavoriteResponse, FavoriteService } from 'src/app/core/services/favorite.service';

@Component({
  selector: 'app-favorites',
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.css'],
})
export class FavoritesComponent implements OnInit {
  favorites: FavoriteResponse[] = [];
  loading = false;
  errorMessage = '';

  constructor(private readonly favoriteService: FavoriteService) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  loadFavorites(): void {
    this.loading = true;
    this.errorMessage = '';
    this.favoriteService.get().subscribe({
      next: (rows) => {
        this.favorites = rows;
        this.loading = false;
      },
      error: () => {
        this.favorites = [];
        this.loading = false;
        this.errorMessage = 'Unable to load favorites.';
      },
    });
  }

  remove(songId: number): void {
    const previous = [...this.favorites];
    this.favorites = this.favorites.filter((song) => song.songId !== songId);

    this.favoriteService.remove(songId).subscribe({
      next: () => {},
      error: () => {
        this.favorites = previous;
        this.errorMessage = 'Unable to remove favorite right now.';
      },
    });
  }
}
