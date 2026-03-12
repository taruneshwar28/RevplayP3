import { of, throwError } from 'rxjs';

import { MusicPlayerComponent } from './music-player.component';
import { ListeningHistoryService } from 'src/app/core/services/listening-history.service';
import { SongLibraryService, SongCatalogResponse } from 'src/app/core/services/song-library.service';
import { PlayResponse } from 'src/app/core/services/listening-history.service';

class AudioMock {
  currentTime = 0;
  duration = 0;
  volume = 1;
  src = '';

  load = jasmine.createSpy('load');
  play = jasmine.createSpy('play').and.returnValue(Promise.resolve());
  pause = jasmine.createSpy('pause');

  addEventListener(): void {}
  removeEventListener(): void {}
}

describe('MusicPlayerComponent (simple Jasmine tests)', () => {
  let component: MusicPlayerComponent;
  let songLibraryServiceSpy: jasmine.SpyObj<SongLibraryService>;
  let listeningHistorySpy: jasmine.SpyObj<ListeningHistoryService>;
  let audioMock: AudioMock;
  let querySongId: string | null;

  const songs: SongCatalogResponse[] = [
    { id: 10, title: 'Song One', artistId: 1, artistName: 'Artist One', duration: 120, fileUrl: '/song-1.mp3' },
    { id: 20, title: 'Song Two', artistId: 2, artistName: 'Artist Two', duration: 180, fileUrl: '/song-2.mp3' }
  ];

  function createComponent(): MusicPlayerComponent {
    const routeStub = {
      snapshot: {
        queryParamMap: {
          get: (key: string) => (key === 'songId' ? querySongId : null)
        }
      }
    };

    return new MusicPlayerComponent(songLibraryServiceSpy, listeningHistorySpy, routeStub as any);
  }

  beforeEach(() => {
    songLibraryServiceSpy = jasmine.createSpyObj<SongLibraryService>('SongLibraryService', ['getPublicSongs']);
    listeningHistorySpy = jasmine.createSpyObj<ListeningHistoryService>('ListeningHistoryService', ['recordPlay']);
    songLibraryServiceSpy.getPublicSongs.and.returnValue(of({ content: songs, page: 0, pageSize: 500, totalElements: 2, totalPages: 1 }));
    const playResponse: PlayResponse = {
      songId: 10,
      title: 'Song One',
      artistName: 'Artist One',
      albumTitle: 'Album One',
      fileUrl: '/song-1.mp3',
      duration: 120,
    };
    listeningHistorySpy.recordPlay.and.returnValue(of(playResponse));

    querySongId = null;
    audioMock = new AudioMock();
    spyOn(window as any, 'Audio').and.returnValue(audioMock);

    component = createComponent();
  });

  it('should create and load first song on init', () => {
    component.ngOnInit();

    expect(component).toBeTruthy();
    expect(component.songs.length).toBe(2);
    expect(component.currentSongIndex).toBe(0);
    expect(audioMock.src).toBe('/song-1.mp3');
    expect(audioMock.load).toHaveBeenCalled();
  });

  it('should autoplay selected song from query param', async () => {
    querySongId = '20';
    component = createComponent();

    component.ngOnInit();
    await Promise.resolve();

    expect(component.currentSongIndex).toBe(1);
    expect(audioMock.play).toHaveBeenCalled();
    expect(component.isPlaying).toBeTrue();
    expect(listeningHistorySpy.recordPlay).toHaveBeenCalledWith(20);
  });

  it('should show load error when songs fail to load', () => {
    songLibraryServiceSpy.getPublicSongs.and.returnValue(throwError(() => new Error('network')));
    component = createComponent();

    component.ngOnInit();

    expect(component.songs).toEqual([]);
    expect(component.loadError).toBe('No songs to play yet.');
  });

  it('should pause when playPause is pressed while playing', () => {
    component.ngOnInit();
    component.isPlaying = true;

    component.playPause();

    expect(audioMock.pause).toHaveBeenCalled();
    expect(component.isPlaying).toBeFalse();
  });
});
