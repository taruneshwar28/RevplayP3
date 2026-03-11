import { Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { catchError, of, switchMap, throwError } from 'rxjs';
import { ArtistService } from 'src/app/core/services/artist.service';
import { SongRequest, SongService, SongVisibility } from 'src/app/core/services/song.service';

@Component({
  selector: 'app-upload-song',
  templateUrl: './upload-song.component.html',
  styleUrls: ['./upload-song.component.css'],
})
export class UploadSongComponent implements OnDestroy {
  @ViewChild('audioFileInput') audioFileInput?: ElementRef<HTMLInputElement>;
  private readonly registeredFirstName = (localStorage.getItem('firstName') ?? '').trim();
  uploadForm: FormGroup;
  errorMessage = '';
  successMessage = '';
  selectedAudioName = '';
  private selectedAudioFile: File | null = null;
  private selectedAudioPreviewUrl: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly songService: SongService,
    private readonly artistService: ArtistService
  ) {
    this.uploadForm = this.fb.group({
      title: ['', Validators.required],
      genre: ['', Validators.required],
      duration: [180, [Validators.required, Validators.min(1)]],
      visibility: ['PUBLIC', Validators.required],
    });
  }

  onAudioFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const selectedFile = input.files?.[0];

    if (!selectedFile) {
      return;
    }

    if (!selectedFile.type.startsWith('audio/')) {
      this.errorMessage = 'Please choose a valid audio file.';
      input.value = '';
      return;
    }

    this.errorMessage = '';
    this.selectedAudioName = selectedFile.name;
    this.selectedAudioFile = selectedFile;
    this.revokeSelectedAudioPreviewUrl();
    this.selectedAudioPreviewUrl = URL.createObjectURL(selectedFile);
    this.updateDurationFromFile(this.selectedAudioPreviewUrl);
  }

  uploadSong(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    if (!this.selectedAudioFile) {
      this.errorMessage = 'Please choose an audio file.';
      return;
    }

    this.readFileAsDataUrl(this.selectedAudioFile)
      .then((fileUrl) => {
        const form = this.uploadForm.value;
        const payload: SongRequest = {
          title: form.title?.trim(),
          genre: form.genre?.trim(),
          duration: Number(form.duration),
          visibility: form.visibility as SongVisibility,
          fileUrl,
        };

        this.ensureArtistProfileExists()
          .pipe(switchMap(() => this.songService.uploadSong(payload)))
          .subscribe({
            next: () => {
              this.successMessage = 'Song uploaded successfully';
              this.selectedAudioFile = null;
              this.revokeSelectedAudioPreviewUrl();
              this.selectedAudioName = '';
              if (this.audioFileInput?.nativeElement) {
                this.audioFileInput.nativeElement.value = '';
              }
              this.uploadForm.reset({ visibility: 'PUBLIC', duration: 180 });
            },
            error: (err) => {
              const validationErrors = err?.error?.errors as Record<string, string> | undefined;
              if (validationErrors && Object.keys(validationErrors).length > 0) {
                this.errorMessage = Object.values(validationErrors)[0];
                return;
              }
              const backendMessage =
                err?.error?.message ||
                err?.error?.error ||
                (typeof err?.error === 'string' ? err.error : '');
              if (backendMessage) {
                this.errorMessage = backendMessage;
                return;
              }
              const statusCode = err?.status;
              this.errorMessage = statusCode ? `Upload failed (HTTP ${statusCode})` : 'Upload failed';
            },
          });
      })
      .catch(() => {
        this.errorMessage = 'Failed to read the selected audio file.';
      });
  }

  ngOnDestroy(): void {
    this.revokeSelectedAudioPreviewUrl();
  }

  private updateDurationFromFile(audioUrl: string): void {
    const audio = new Audio();
    audio.preload = 'metadata';
    audio.src = audioUrl;

    audio.onloadedmetadata = () => {
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        this.uploadForm.patchValue({ duration: Math.ceil(audio.duration) });
      }
    };
  }

  private revokeSelectedAudioPreviewUrl(): void {
    if (this.selectedAudioPreviewUrl) {
      URL.revokeObjectURL(this.selectedAudioPreviewUrl);
      this.selectedAudioPreviewUrl = null;
    }
  }

  private readFileAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result;
        if (typeof result === 'string') {
          resolve(result);
          return;
        }
        reject(new Error('Invalid file content'));
      };
      reader.onerror = () => reject(reader.error ?? new Error('Failed to read file'));
      reader.readAsDataURL(file);
    });
  }

  private ensureArtistProfileExists() {
    return this.artistService.getMyProfile().pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status !== 404) {
          return throwError(() => err);
        }

        return this.artistService.createProfile({
          stageName: this.registeredFirstName || 'Artist',
          bio: '',
          genre: '',
          instagramUrl: '',
          twitterUrl: '',
          youtubeUrl: '',
          websiteUrl: '',
          profileImageUrl: '',
        });
      }),
      switchMap(() => of(void 0))
    );
  }
}
