import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { FavoriteService } from 'src/app/core/services/favorite.service';
import {
  SongCatalogResponse,
  SongLibraryService,
} from 'src/app/core/services/song-library.service';
import { BrowseComponent } from './browse.component';

describe('BrowseComponent', () => {
  let component: BrowseComponent;
  let fixture: ComponentFixture<BrowseComponent>;
  let routerSpy: jasmine.SpyObj<Router>;
  let songLibraryServiceSpy: jasmine.SpyObj<SongLibraryService>;
  let favoriteServiceSpy: jasmine.SpyObj<FavoriteService>;

  const songs: SongCatalogResponse[] = [
    {
      id: 1,
      title: 'Alpha',
      artistId: 10,
      artistName: 'Artist One',
      albumTitle: 'Album A',
      duration: 120,
      genre: 'Pop',
      fileUrl: '/alpha.mp3',
      playCount: 20,
      createdAt: '2026-03-01T10:00:00',
    },
    {
      id: 2,
      title: 'Beta',
      artistId: 11,
      artistName: 'Artist Two',
      albumTitle: 'Album B',
      duration: 180,
      genre: 'Rock',
      fileUrl: '/beta.mp3',
      playCount: 5,
      createdAt: '2026-03-02T10:00:00',
    },
  ];

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);
    songLibraryServiceSpy = jasmine.createSpyObj<SongLibraryService>('SongLibraryService', [
      'getPublicSongs',
      'searchSongs',
    ]);
    favoriteServiceSpy = jasmine.createSpyObj<FavoriteService>('FavoriteService', [
      'getCachedFavoriteSongIds',
      'get',
      'add',
      'remove',
    ]);

    songLibraryServiceSpy.getPublicSongs.and.returnValue(
      of({
        content: songs,
        page: 0,
        pageSize: 5,
        totalElements: 2,
        totalPages: 1,
      })
    );
    songLibraryServiceSpy.searchSongs.and.returnValue(
      of({
        songs,
        totalResults: 2,
        page: 0,
        pageSize: 5,
      })
    );
    favoriteServiceSpy.getCachedFavoriteSongIds.and.returnValue(new Set<number>([2]));
    favoriteServiceSpy.get.and.returnValue(
      of([
        {
          id: 1,
          songId: 2,
          songTitle: 'Beta',
          artistName: 'Artist Two',
          addedAt: '2026-03-02T10:00:00',
        },
      ])
    );
    favoriteServiceSpy.add.and.returnValue(of(void 0));
    favoriteServiceSpy.remove.and.returnValue(of(void 0));

    TestBed.configureTestingModule({
      declarations: [BrowseComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(
              convertToParamMap({
                q: '',
                artist: '',
                album: '',
                genre: '',
                date: '',
                sort: 'none',
                page: '1',
              })
            ),
          },
        },
        { provide: Router, useValue: routerSpy },
        { provide: SongLibraryService, useValue: songLibraryServiceSpy },
        { provide: FavoriteService, useValue: favoriteServiceSpy },
      ],
    });

    fixture = TestBed.createComponent(BrowseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load public songs on init', () => {
    expect(songLibraryServiceSpy.getPublicSongs).toHaveBeenCalledWith(0, 5);
    expect(component.songs.length).toBe(2);
    expect(component.visibleSongs.length).toBe(2);
    expect(component.songs[1].isFavorite).toBeTrue();
  });

  it('should navigate to song details', () => {
    component.viewSongDetails(2);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/browse', 2], {
      queryParamsHandling: 'preserve',
    });
  });

  it('should navigate to player page', () => {
    component.playSong(1);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/player'], {
      queryParams: { songId: 1 },
    });
  });

  it('should add a favorite and keep optimistic state on success', () => {
    component.toggleFavorite(1, false);

    expect(favoriteServiceSpy.add).toHaveBeenCalledWith(1, 'Alpha', 'Artist One');
    expect(component.songs.find((song) => song.id === 1)?.isFavorite).toBeTrue();
  });

  it('should revert favorite state when adding fails', () => {
    favoriteServiceSpy.add.and.returnValue(throwError(() => new Error('add failed')));

    component.toggleFavorite(1, false);

    expect(component.songs.find((song) => song.id === 1)?.isFavorite).toBeFalse();
  });
});
