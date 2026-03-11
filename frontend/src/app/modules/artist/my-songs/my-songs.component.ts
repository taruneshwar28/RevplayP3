import { Component, OnInit } from '@angular/core';
import { SongResponse, SongService, SongVisibility } from 'src/app/core/services/song.service';

@Component({
  selector: 'app-my-songs',
  templateUrl: './my-songs.component.html',
  styleUrls: ['./my-songs.component.css'],
})
export class MySongsComponent implements OnInit {
  songs: SongResponse[] = [];
  editingSongId: number | null = null;
  errorMessage = '';
  successMessage = '';
  editForm: Partial<SongResponse> = {
    title: '',
    genre: '',
    duration: 0,
    visibility: 'PUBLIC',
  };

  constructor(private readonly songService: SongService) {}

  ngOnInit(): void {
    this.loadSongs();
  }

  loadSongs(): void {
    this.errorMessage = '';
    this.songService.getSongs().subscribe({
      next: (res) => {
        this.songs = res;
      },
      error: () => {
        this.songs = [];
      },
    });
  }

  startEdit(song: SongResponse): void {
    this.editingSongId = song.id;
    this.editForm = {
      title: song.title,
      genre: song.genre,
      duration: song.duration,
      visibility: song.visibility,
    };
  }

  cancelEdit(): void {
    this.editingSongId = null;
  }

  updateSong(songId: number): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.songService
      .updateSong(songId, {
        title: this.editForm.title,
        genre: this.editForm.genre,
        duration: Number(this.editForm.duration),
        visibility: this.editForm.visibility as SongVisibility,
      })
      .subscribe({
        next: (updatedSong) => {
          this.songs = this.songs.map((song) => (song.id === songId ? updatedSong : song));
          this.editingSongId = null;
          this.successMessage = 'Song updated successfully.';
        },
        error: (err) => {
          this.errorMessage = this.getErrorMessage(err, 'Failed to update the song.');
        },
      });
  }

  deleteSong(songId: number): void {
    if (!confirm('Are you sure to delete this song?')) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.songService.deleteSong(songId).subscribe({
      next: () => {
        this.songs = this.songs.filter((song) => song.id !== songId);
        if (this.editingSongId === songId) {
          this.editingSongId = null;
        }
        this.successMessage = 'Song deleted successfully.';
      },
      error: (err) => {
        this.errorMessage = this.getErrorMessage(err, 'Failed to delete the song.');
      },
    });
  }

  toggleVisibility(song: SongResponse): void {
    this.errorMessage = '';
    this.successMessage = '';
    const newVisibility: SongVisibility = song.visibility === 'PUBLIC' ? 'UNLISTED' : 'PUBLIC';

    this.songService.updateVisibility(song.id, newVisibility).subscribe({
      next: (updatedSong) => {
        this.songs = this.songs.map((s) => (s.id === song.id ? updatedSong : s));
        this.successMessage = 'Visibility updated successfully.';
      },
      error: (err) => {
        this.errorMessage = this.getErrorMessage(err, 'Failed to update visibility.');
      },
    });
  }

  private getErrorMessage(err: any, fallback: string): string {
    const validationErrors = err?.error?.errors as Record<string, string> | undefined;
    if (validationErrors && Object.keys(validationErrors).length > 0) {
      return Object.values(validationErrors)[0];
    }

    return (
      err?.error?.message ||
      err?.error?.error ||
      (typeof err?.error === 'string' ? err.error : '') ||
      fallback
    );
  }
}
