import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MusicPlayerComponent } from './music-player/music-player.component';

const routes: Routes = [
  { path: '', component: MusicPlayerComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PlayerRoutingModule { }
