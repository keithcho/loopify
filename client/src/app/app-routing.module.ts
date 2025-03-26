import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { PlaylistSelectComponent } from './components/playlist-select/playlist-select.component';
import { SongPlayerComponent } from './components/song-player/song-player.component';
import { LandingComponent } from './components/landing/landing.component';
import { TopTracksComponent } from './components/top-tracks/top-tracks.component';
import { TopTracksRecommendationsComponent } from './components/top-tracks-recommendations/top-tracks-recommendations.component';

const routes: Routes = [
  { path: '', redirectTo: '/landing', pathMatch: 'full' },
  { path: 'landing', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent},
  { path: 'playlists', component: PlaylistSelectComponent },
  { path: 'player/:id/:name', component: SongPlayerComponent },
  { path: 'top-tracks', component: TopTracksComponent },
  { path: 'recommendations/top-tracks/:timeRange', component: TopTracksRecommendationsComponent },
  { path: '**', redirectTo: '/landing' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }