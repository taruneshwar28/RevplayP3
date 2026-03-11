import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ArtistProfileResponse } from '../../core/services/artist.service';
import { SongService } from '../../services/song.service';

@Component({
  selector: 'app-artist-profile',
  templateUrl: './artist-profile.component.html',
  styleUrls: ['./artist-profile.component.css']
})
export class ArtistProfileComponent implements OnInit {
  artist: ArtistProfileResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private songService: SongService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.songService.getArtistById(id).subscribe(data => (this.artist = data));
  }
}
