import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/core/services/auth.service';
import {
  UserProfileRequest,
  UserProfileResponse,
  UserService,
  UserStatsResponse,
} from 'src/app/core/services/user.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
})
export class ProfileComponent implements OnInit {
  profile: UserProfileResponse | null = null;
  email = '';
  private readonly registeredFirstName = (localStorage.getItem('firstName') ?? '').trim();
  form: UserProfileRequest = {
    username: '',
    displayName: '',
    bio: '',
  };

  stats: UserStatsResponse = {
    playlistCount: 0,
    favoriteCount: 0,
  };

  loading = false;
  creating = false;
  editing = false;
  errorMessage = '';

  constructor(
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.email = localStorage.getItem('userEmail') ?? '';
    this.applyRegisterDefaults();
    this.loadProfile();
    this.loadStats();
  }

  loadProfile(): void {
    this.errorMessage = '';
    this.userService.getProfile().subscribe({
      next: (res) => {
        this.profile = res;
        this.form = {
          username: this.valueOrFallback(res.username, this.registeredFirstName),
          displayName: this.valueOrFallback(res.displayName, this.registeredFirstName),
          bio: res.bio ?? '',
        };
        this.creating = false;
        this.editing = false;
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.creating = true;
          this.editing = true;
          this.applyRegisterDefaults();
          return;
        }
        this.errorMessage = 'Failed to load profile';
      },
    });
  }

  loadStats(): void {
    this.userService.getStats().subscribe({
      next: (res) => {
        this.stats = res;
      },
      error: () => {
        this.stats = {
          playlistCount: 0,
          favoriteCount: 0,
        };
      },
    });
  }

  save(): void {
    if (!this.form.username.trim() || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const payload: UserProfileRequest = {
      username: this.form.username.trim(),
      displayName: this.form.displayName?.trim() ?? '',
      bio: this.form.bio?.trim() ?? '',
    };

    const request$ = this.creating
      ? this.userService.createProfile(payload)
      : this.userService.updateProfile(payload);

    request$.subscribe({
      next: (res) => {
        this.profile = res;
        this.creating = false;
        this.editing = false;
        this.loading = false;
        this.loadStats();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to save profile';
      },
    });
  }

  onPrimaryAction(): void {
    if (this.loading) {
      return;
    }

    if (this.creating || this.editing) {
      this.save();
      return;
    }

    this.editing = true;
    this.errorMessage = '';
  }

  get primaryButtonLabel(): string {
    if (this.loading) {
      return 'Saving...';
    }

    if (this.creating || this.editing) {
      return 'Save';
    }

    return 'Update';
  }

  get isEditMode(): boolean {
    return this.creating || this.editing;
  }

  private applyRegisterDefaults(): void {
    this.form = {
      username: this.valueOrFallback(this.form.username, this.registeredFirstName),
      displayName: this.valueOrFallback(this.form.displayName, this.registeredFirstName),
      bio: this.form.bio ?? '',
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
