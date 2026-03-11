import { Component, OnInit } from '@angular/core';
import {
  ListeningHistoryItem,
  ListeningHistoryService,
} from '../../core/service/listening-history.service';

@Component({
  selector: 'app-listening-history',
  templateUrl: './listening-history.component.html',
  styleUrls: ['./listening-history.component.css'],
})
export class ListeningHistoryComponent implements OnInit {
  historyItems: ListeningHistoryItem[] = [];
  loading = false;
  errorMessage = '';

  constructor(private readonly listeningHistoryService: ListeningHistoryService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  clearHistory(): void {
    this.loading = true;
    this.errorMessage = '';
    this.listeningHistoryService.clearHistory().subscribe({
      next: () => {
        this.historyItems = [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Unable to clear history right now.';
      },
    });
  }

  trackByHistoryId(index: number, item: ListeningHistoryItem): string {
    return `${item.songId}-${index}`;
  }

  private loadHistory(): void {
    this.loading = true;
    this.errorMessage = '';
    this.listeningHistoryService.getRecentHistory(0, 50).subscribe({
      next: (items) => {
        this.historyItems = this.getUniqueRecentSongs(items);
        this.loading = false;
      },
      error: () => {
        this.historyItems = [];
        this.loading = false;
        this.errorMessage = 'Please log in to view listening history.';
      },
    });
  }

  private getUniqueRecentSongs(items: ListeningHistoryItem[]): ListeningHistoryItem[] {
    const uniqueItems = new Map<number, ListeningHistoryItem>();

    for (const item of items) {
      if (!uniqueItems.has(item.songId)) {
        uniqueItems.set(item.songId, item);
      }
    }

    return Array.from(uniqueItems.values()).slice(0, 50);
  }
}
