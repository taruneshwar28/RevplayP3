import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ElementRef } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AnalyticsService, DailyTrendData, TopListenerResponse } from 'src/app/core/services/analytics.service';
import { ArtistProfileResponse, ArtistService } from 'src/app/core/services/artist.service';
import { FavoriteService } from 'src/app/core/services/favorite.service';
import { SongResponse, SongService } from 'src/app/core/services/song.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let analyticsServiceSpy: jasmine.SpyObj<AnalyticsService>;
  let artistServiceSpy: jasmine.SpyObj<ArtistService>;
  let favoriteServiceSpy: jasmine.SpyObj<FavoriteService>;
  let songServiceSpy: jasmine.SpyObj<SongService>;

  const artistProfile: ArtistProfileResponse = {
    id: 12,
    userId: 12,
    stageName: 'Artist',
    verified: true,
    songCount: 2,
    albumCount: 1,
    totalPlays: 50,
    createdAt: '2026-03-01T00:00:00',
  };

  const uploadedSongs: SongResponse[] = [
    {
      id: 1,
      title: 'Song One',
      artistId: 12,
      artistName: 'Artist',
      duration: 120,
      fileUrl: '/one.mp3',
      visibility: 'PUBLIC',
      playCount: 10,
      createdAt: '2026-03-01T00:00:00',
    },
    {
      id: 2,
      title: 'Song Two',
      artistId: 12,
      artistName: 'Artist',
      duration: 180,
      fileUrl: '/two.mp3',
      visibility: 'PUBLIC',
      playCount: 25,
      createdAt: '2026-03-02T00:00:00',
    },
  ];

  const trends: DailyTrendData[] = [
    { date: '2026-03-01', playCount: 5, uniqueListeners: 2, totalMinutes: 12.5 },
    { date: '2026-03-02', playCount: 8, uniqueListeners: 3, totalMinutes: 16.0 },
  ];

  const listeners: TopListenerResponse[] = [
    { userId: 1, username: 'listener-1', playCount: 9, totalListeningMinutes: 15 },
  ];

  beforeEach(() => {
    analyticsServiceSpy = jasmine.createSpyObj<AnalyticsService>('AnalyticsService', ['getTrends', 'getTopListeners']);
    artistServiceSpy = jasmine.createSpyObj<ArtistService>('ArtistService', ['getMyProfile']);
    favoriteServiceSpy = jasmine.createSpyObj<FavoriteService>('FavoriteService', ['getCounts']);
    songServiceSpy = jasmine.createSpyObj<SongService>('SongService', ['getSongs']);

    analyticsServiceSpy.getTrends.and.returnValue(of({ trends }));
    analyticsServiceSpy.getTopListeners.and.returnValue(of(listeners));
    artistServiceSpy.getMyProfile.and.returnValue(of(artistProfile));
    favoriteServiceSpy.getCounts.and.returnValue(of({ 1: 2, 2: 4 }));
    songServiceSpy.getSongs.and.returnValue(of(uploadedSongs));

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      providers: [
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: ArtistService, useValue: artistServiceSpy },
        { provide: FavoriteService, useValue: favoriteServiceSpy },
        { provide: SongService, useValue: songServiceSpy },
      ],
    });

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    component.trendChartRef = new ElementRef(document.createElement('canvas'));
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.removeItem('artistId');
    component.ngOnDestroy();
  });

  it('should create and load uploaded songs, favorites, profile, trends, and listeners', () => {
    expect(component).toBeTruthy();
    expect(songServiceSpy.getSongs).toHaveBeenCalled();
    expect(artistServiceSpy.getMyProfile).toHaveBeenCalled();
    expect(analyticsServiceSpy.getTrends).toHaveBeenCalled();
    expect(analyticsServiceSpy.getTopListeners).toHaveBeenCalledWith(12, 5);
    expect(component.uploadedSongs.length).toBe(2);
    expect(component.songs[0].songId).toBe(2);
    expect(component.topListeners).toEqual(listeners);
    expect(component.getFavoriteCount(1)).toBe(2);
    expect(component.getFavoriteCount(2)).toBe(4);
  });

  it('should compute total favorites and total plays from loaded data', () => {
    expect(component.getTotalFavorites()).toBe(6);
    expect(component.getTotalPlays()).toBe(35);
  });

  it('should clear uploaded song state when song loading fails', () => {
    songServiceSpy.getSongs.and.returnValue(throwError(() => new Error('songs failed')));

    component.loadUploadedSongs();

    expect(component.uploadedSongs).toEqual([]);
    expect(component.songs).toEqual([]);
    expect(component.topSongs).toEqual([]);
    expect(component.getTotalFavorites()).toBe(0);
  });

  it('should reset favorite counts when favorite lookup fails', () => {
    favoriteServiceSpy.getCounts.and.returnValue(throwError(() => new Error('favorites failed')));

    component.loadUploadedSongs();

    expect(component.getFavoriteCount(1)).toBe(0);
    expect(component.getFavoriteCount(2)).toBe(0);
    expect(component.topSongs.length).toBe(2);
  });

  it('should clear trends and listeners when profile lookup fails', () => {
    artistServiceSpy.getMyProfile.and.returnValue(throwError(() => new Error('profile failed')));

    component.ngOnInit();

    expect(component.topListeners).toEqual([]);
    expect(component.trends).toEqual([]);
  });

  it('should update period and reload trends for valid values', () => {
    component.onPeriodChange('monthly');

    expect(component.period).toBe('MONTHLY');
    expect(analyticsServiceSpy.getTrends).toHaveBeenCalled();
  });

  it('should fall back to no listeners when top listener lookup fails', () => {
    analyticsServiceSpy.getTopListeners.and.returnValue(throwError(() => new Error('listeners failed')));

    component['loadTopListeners']();

    expect(component.topListeners).toEqual([]);
  });
});
