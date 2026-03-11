import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ArtistProfileRequest, ArtistProfileResponse, ArtistService } from 'src/app/core/services/artist.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  profile: ArtistProfileResponse | null = null;
  private readonly registeredFirstName = (localStorage.getItem('firstName') ?? '').trim();
  form: ArtistProfileRequest = {
    stageName: '',
    bio: '',
    genre: '',
    instagramUrl: '',
    twitterUrl: '',
    youtubeUrl: '',
    websiteUrl: '',
    profileImageUrl: '',
  };
  isEditMode = false;
  creating = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private readonly artistService: ArtistService,
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.applyRegisterDefaults();
    this.loadProfile();
  }

  loadProfile(): void {
    this.errorMessage = '';
    this.artistService.getMyProfile().subscribe({
      next: (res) => {
        this.applyProfile(res);
        this.creating = false;
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.creating = true;
          this.profile = null;
          this.applyRegisterDefaults();
          return;
        }
      },
    });
  }

  saveProfile(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.form.stageName.trim()) {
      this.errorMessage = 'Stage name is required';
      return;
    }

    const payload: ArtistProfileRequest = {
      stageName: this.form.stageName.trim(),
      bio: this.form.bio?.trim() ?? '',
      genre: this.form.genre?.trim() ?? '',
      instagramUrl: this.normalizeUrl(this.form.instagramUrl),
      twitterUrl: this.normalizeUrl(this.form.twitterUrl),
      youtubeUrl: this.normalizeUrl(this.form.youtubeUrl),
      websiteUrl: this.normalizeUrl(this.form.websiteUrl),
      profileImageUrl: this.form.profileImageUrl?.trim() ?? '',
    };

    const request$ = this.creating
      ? this.artistService.createProfile(payload)
      : this.artistService.updateMyProfile(payload);

    request$.subscribe({
      next: (res) => {
        this.applyProfile(res);
        this.creating = false;
        this.isEditMode = false;
        this.successMessage = 'Profile updated successfully';
        this.loadProfile();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to save profile';
      },
    });
  }

  enableEdit(): void {
    this.isEditMode = true;
  }

  cancelEdit(): void {
    this.isEditMode = false;
    if (this.profile) {
      this.form = {
        stageName: this.profile.stageName,
        bio: this.profile.bio ?? '',
        genre: this.profile.genre ?? '',
        instagramUrl: this.profile.instagramUrl ?? '',
        twitterUrl: this.profile.twitterUrl ?? '',
        youtubeUrl: this.profile.youtubeUrl ?? '',
        websiteUrl: this.profile.websiteUrl ?? '',
        profileImageUrl: this.profile.profileImageUrl ?? '',
      };
    }
  }

  openLink(url?: string): void {
    if (!url) {
      return;
    }

    window.open(url, '_blank', 'noopener,noreferrer');
  }

  private normalizeUrl(value?: string): string {
    const trimmed = value?.trim() ?? '';
    if (!trimmed) {
      return '';
    }

    if (/^https?:\/\//i.test(trimmed)) {
      return trimmed;
    }

    return `https://${trimmed}`;
  }

  private applyProfile(profile: ArtistProfileResponse): void {
    this.profile = profile;
    this.form = {
      stageName: this.valueOrFallback(profile.stageName, this.registeredFirstName),
      bio: profile.bio ?? '',
      genre: profile.genre ?? '',
      instagramUrl: profile.instagramUrl ?? '',
      twitterUrl: profile.twitterUrl ?? '',
      youtubeUrl: profile.youtubeUrl ?? '',
      websiteUrl: profile.websiteUrl ?? '',
      profileImageUrl: profile.profileImageUrl ?? '',
    };
    localStorage.setItem('artistId', String(profile.id));
  }

  private applyRegisterDefaults(): void {
    this.form = {
      ...this.form,
      stageName: this.valueOrFallback(this.form.stageName, this.registeredFirstName),
    };
  }

  private valueOrFallback(value?: string, fallback = ''): string {
    const trimmed = value?.trim() ?? '';
    return trimmed || fallback;
  }

  logout(): void {
    this.authService.logout(false);
    this.router.navigate(['/home/login']);
  }
}
