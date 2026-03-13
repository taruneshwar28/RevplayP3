import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { BrowseRoutingModule } from './browse-routing.module';
import { BrowseComponent } from './browse.component';
import { SongDetailComponent } from './song-detail.component';
import { SearchBarComponent } from './search-bar/search-bar.component';

@NgModule({
  declarations: [BrowseComponent, SongDetailComponent, SearchBarComponent],
  imports: [CommonModule, FormsModule, BrowseRoutingModule],
})
export class BrowseModule {}
