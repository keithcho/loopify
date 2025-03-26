import { NgModule, isDevMode } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { provideHttpClient } from '@angular/common/http';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { PlaylistSelectComponent } from './components/playlist-select/playlist-select.component';
import { SongPlayerComponent } from './components/song-player/song-player.component';
import { LandingComponent } from './components/landing/landing.component';
import { FormsModule } from '@angular/forms';
import { CustomPromptComponent } from './components/custom-prompt/custom-prompt.component';
import { TopTracksComponent } from './components/top-tracks/top-tracks.component';
import { TopTracksRecommendationsComponent } from './components/top-tracks-recommendations/top-tracks-recommendations.component';
import { LoadingAnimationComponent } from './components/loading-animation/loading-animation.component';
import { MusicCarouselComponent } from './components/music-carousel/music-carousel.component';
import { ServiceWorkerModule } from '@angular/service-worker';
import { environment } from '../environments/environment';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    PlaylistSelectComponent,
    SongPlayerComponent,
    LandingComponent,
    CustomPromptComponent,
    TopTracksComponent,
    TopTracksRecommendationsComponent,
    LoadingAnimationComponent,
    MusicCarouselComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ServiceWorkerModule.register('ngsw-worker.js', {
      enabled: environment.production,
      // Register the ServiceWorker as soon as the application is stable
      // or after 30 seconds (whichever comes first).
      registrationStrategy: 'registerWhenStable:30000'
    })
  ],
  providers: [
    provideHttpClient()
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
