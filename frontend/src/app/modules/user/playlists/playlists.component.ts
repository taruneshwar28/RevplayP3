import { Component, OnInit } from '@angular/core';
import { PlaylistResponse, PlaylistService } from 'src/app/core/services/playlist.service';
import { SongCatalogResponse, SongLibraryService } from 'src/app/core/services/song-library.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-playlists',
  templateUrl: './playlists.component.html',
  styleUrls: ['./playlists.component.css'],
})
export class PlaylistsComponent implements OnInit {
  availableSongs: SongCatalogResponse[] = [];
  playlists: PlaylistResponse[] = [];

  newPlaylist = { name: '', description: '' };
  editDrafts: Record<number, { name: string; description: string }> = {};
  songInput: Record<number, number | null> = {};
  loading = false;
  saving = false;
  errorMessage = '';

  constructor(
    private readonly playlistService: PlaylistService,
    private readonly songLibraryService: SongLibraryService,
  ) {}

  ngOnInit(): void {
    this.loadSongs();
  }

  loadSongs(): void {
    this.loading = true;
    this.songLibraryService.getPublicSongs(0, 200).subscribe({
      next: (songsPage) => {
        this.availableSongs = songsPage.content;
        this.load();
      },
      error: () => {
        this.availableSongs = [];
        this.load();
      },
    });
  }

  load(): void {
    this.errorMessage = '';
    this.playlistService.getAll().subscribe({
      next: (res) => {
        if (res.length === 0) {
          this.playlists = [];
          this.loading = false;
          return;
        }

        forkJoin(
          res.map((playlist) =>
            this.playlistService.getById(playlist.id)
          )
        ).subscribe({
          next: (details) => {
            this.playlists = details;
            this.playlists.forEach((playlist) => {
              this.editDrafts[playlist.id] = {
                name: playlist.name,
                description: playlist.description ?? '',
              };
              if (this.songInput[playlist.id] === undefined) {
                this.songInput[playlist.id] = this.availableSongs.length > 0 ? this.availableSongs[0].id : null;
              }
            });
            this.loading = false;
          },
          error: () => {
            this.playlists = res.map((playlist) => ({ ...playlist, songs: playlist.songs ?? [] }));
            this.loading = false;
            this.errorMessage = 'Unable to load playlist songs.';
          },
        });
      },
      error: () => {
        this.playlists = [];
        this.loading = false;
        this.errorMessage = 'Unable to load playlists.';
      },
    });
  }

  canCreatePlaylist(): boolean {
    return this.newPlaylist.name.trim().length > 0;
  }

  create(): void {
    const name = this.newPlaylist.name.trim();
    if (!name) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.playlistService
      .create({
        name,
        description: this.newPlaylist.description.trim(),
      })
      .subscribe({
        next: () => {
          this.newPlaylist = { name: '', description: '' };
          this.saving = false;
          this.load();
        },
        error: () => {
          this.saving = false;
          this.errorMessage = 'Unable to create playlist.';
        },
      });
  }

  update(playlistId: number): void {
    const draft = this.editDrafts[playlistId];
    if (!draft || !draft.name.trim()) {
      this.errorMessage = 'Playlist name is required.';
      return;
    }

    this.errorMessage = '';
    this.playlistService.update(playlistId, {
      name: draft.name.trim(),
      description: draft.description.trim(),
    }).subscribe({
      next: () => this.load(),
      error: () => {
        this.errorMessage = 'Unable to update playlist.';
      },
    });
  }

  delete(playlistId: number): void {
    this.errorMessage = '';
    this.playlistService.delete(playlistId).subscribe({
      next: () => this.load(),
      error: () => {
        this.errorMessage = 'Unable to delete playlist.';
      },
    });
  }

  addSong(playlistId: number): void {
    const songId = this.songInput[playlistId];
    if (!songId) {
      return;
    }

    this.errorMessage = '';
    this.playlistService.addSong(playlistId, { songId }).subscribe({
      next: () => this.load(),
      error: () => {
        this.errorMessage = 'Unable to add song to playlist.';
      },
    });
  }

  removeSong(playlistId: number, songId: number): void {
    this.errorMessage = '';
    this.playlistService.removeSong(playlistId, songId).subscribe({
      next: () => this.load(),
      error: () => {
        this.errorMessage = 'Unable to remove song from playlist.';
      },
    });
  }
}
