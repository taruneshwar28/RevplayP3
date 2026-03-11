import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import {
  AnalyticsService,
  ListeningTrendResponse,
  SongPerformanceResponse,
  TopListenerResponse,
} from 'src/app/core/services/analytics.service';
import { ArtistService } from 'src/app/core/services/artist.service';
import { FavoriteService } from 'src/app/core/services/favorite.service';
import { SongResponse, SongService } from 'src/app/core/services/song.service';

@Component({
  selector: 'app-artist-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('trendChart') trendChartRef?: ElementRef<HTMLCanvasElement>;

  artistId = Number(localStorage.getItem('artistId')) || 0;
  private trendChart: Chart | null = null;

  songs: SongPerformanceResponse[] = [];
  uploadedSongs: SongResponse[] = [];
  topSongs: SongPerformanceResponse[] = [];
  topListeners: TopListenerResponse[] = [];
  trends: ListeningTrendResponse['trends'] = [];
  favoriteCounts = new Map<number, number>();

  period: 'ALL_TIME' | 'DAILY' | 'WEEKLY' | 'MONTHLY' = 'ALL_TIME';

  constructor(
    private readonly analyticsService: AnalyticsService,
    private readonly artistService: ArtistService,
    private readonly favoriteService: FavoriteService,
    private readonly songService: SongService
  ) {
    Chart.register(...registerables);
  }

  ngOnInit(): void {
    this.loadUploadedSongs();
    this.resolveArtistAndLoadAnalytics();
  }

  ngAfterViewInit(): void {
    this.renderTrendChart();
  }

  ngOnDestroy(): void {
    this.destroyTrendChart();
  }

  loadUploadedSongs(): void {
    this.songService.getSongs().subscribe({
      next: (data) => {
        this.uploadedSongs = data;
        this.rebuildSongMetrics();
        this.loadFavoriteCounts();
      },
      error: () => {
        this.uploadedSongs = [];
        this.songs = [];
        this.topSongs = [];
        this.favoriteCounts = new Map<number, number>();
      },
    });
  }

  loadTrends(): void {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 30);

    this.analyticsService
      .getTrends(this.artistId, start.toISOString().slice(0, 10), end.toISOString().slice(0, 10))
      .subscribe({
        next: (data) => {
          this.trends = data.trends;
          this.renderTrendChart();
        },
      });
  }

  onPeriodChange(value: string): void {
    const normalized = (value || 'ALL_TIME').toUpperCase();
    if (normalized === 'DAILY' || normalized === 'WEEKLY' || normalized === 'MONTHLY' || normalized === 'ALL_TIME') {
      this.period = normalized;
    }
    this.loadTrends();
  }

  getFavoriteCount(songId: number): number {
    return this.favoriteCounts.get(songId) ?? 0;
  }

  getTotalFavorites(): number {
    return Array.from(this.favoriteCounts.values()).reduce((sum, count) => sum + count, 0);
  }

  getTotalPlays(): number {
    return this.uploadedSongs.reduce((sum, song) => sum + (song.playCount ?? 0), 0);
  }

  private resolveArtistAndLoadAnalytics(): void {
    this.artistService.getMyProfile().subscribe({
      next: (profile) => {
        this.artistId = profile.id;
        localStorage.setItem('artistId', String(profile.id));
        this.loadTrends();
        this.loadTopListeners();
      },
      error: () => {
        this.topListeners = [];
        this.trends = [];
      },
    });
  }

  private loadTopListeners(): void {
    if (!this.artistId) {
      this.topListeners = [];
      return;
    }
    this.analyticsService.getTopListeners(this.artistId, 5).subscribe({
      next: (data) => {
        this.topListeners = data ?? [];
      },
      error: () => {
        this.topListeners = [];
      },
    });
  }

  private loadFavoriteCounts(): void {
    const songIds = this.uploadedSongs.map((song) => song.id);
    if (songIds.length === 0) {
      this.favoriteCounts = new Map<number, number>();
      this.rebuildSongMetrics();
      return;
    }

    this.favoriteService.getCounts(songIds).subscribe({
      next: (data) => {
        const counts = new Map<number, number>();
        for (const songId of songIds) {
          counts.set(songId, Number((data as Record<string, number>)[songId] ?? 0));
        }
        this.favoriteCounts = counts;
        this.rebuildSongMetrics();
      },
      error: () => {
        this.favoriteCounts = new Map<number, number>();
        this.rebuildSongMetrics();
      },
    });
  }

  private rebuildSongMetrics(): void {
    const rows: SongPerformanceResponse[] = this.uploadedSongs.map((song) => ({
      songId: song.id,
      title: song.title,
      playCount: song.playCount ?? 0,
      uniqueListeners: 0,
      averageListenDuration: 0,
      completionRate: 0,
    }));

    this.songs = [...rows].sort((a, b) => b.playCount - a.playCount);
    this.topSongs = [...this.songs].slice(0, 10);
  }

  private renderTrendChart(): void {
    if (!this.trendChartRef?.nativeElement) {
      return;
    }

    this.destroyTrendChart();

    const labels = this.trends.map((trend) => trend.date);
    const values = this.trends.map((trend) => trend.playCount);

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Plays',
            data: values,
            borderColor: '#60a5fa',
            backgroundColor: 'rgba(96, 165, 250, 0.18)',
            fill: true,
            tension: 0.35,
            pointRadius: 3,
            pointHoverRadius: 5,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false,
          },
        },
        scales: {
          x: {
            ticks: {
              color: '#94a3b8',
              maxRotation: 0,
              autoSkip: true,
            },
            grid: {
              color: 'rgba(148, 163, 184, 0.08)',
            },
          },
          y: {
            beginAtZero: true,
            ticks: {
              color: '#94a3b8',
              precision: 0,
            },
            grid: {
              color: 'rgba(148, 163, 184, 0.08)',
            },
          },
        },
      },
    };

    this.trendChart = new Chart(this.trendChartRef.nativeElement, config);
  }

  private destroyTrendChart(): void {
    if (this.trendChart) {
      this.trendChart.destroy();
      this.trendChart = null;
    }
  }
}
