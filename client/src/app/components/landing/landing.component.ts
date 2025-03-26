import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface Album {
  url: string;
  color: string;
}

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  standalone: false,
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit, OnDestroy {
  albums: Album[] = [
    { url: "albums/album1.jpeg", color: "#3E2723" },
    { url: "albums/album2.jpeg", color: "#880E4F" },
    { url: "albums/album3.jpeg", color: "#1A237E" },
    { url: "albums/album4.jpeg", color: "#004D40" },
    { url: "albums/album5.jpeg", color: "#BF360C" },
    { url: "albums/album6.jpeg", color: "#263238" }
  ];

  currentIndex = 0;
  carouselInterval: any;
  
  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const urlParams = new URLSearchParams(window.location.search);
    const logoutSuccess = urlParams.get('logout');
    
    if (logoutSuccess === 'success') {
      const url = new URL(window.location.href);
      url.searchParams.delete('logout');
      window.history.replaceState({}, document.title, url.pathname);
    } else {
      // Check if user is already authenticated
      this.authService.isAuthenticated$.subscribe(isAuthenticated => {
        if (isAuthenticated) {
          this.router.navigate(['/dashboard']);
        }
      });
    }
  
    this.startAutoScroll();
    this.initBackgroundShapes();
  }

  ngOnDestroy(): void {
    this.stopAutoScroll();
  }

  startAutoScroll(): void {
    // Use a slightly longer interval for a more deliberate pace
    this.carouselInterval = setInterval(() => {
      // Calculate the next index
      this.currentIndex = (this.currentIndex + 1) % this.albums.length;
    }, 3000); // Increase to 3 seconds for a more comfortable viewing pace
  }

  stopAutoScroll(): void {
    if (this.carouselInterval) {
      clearInterval(this.carouselInterval);
    }
  }

  getBackgroundStyle(): any {
    const color = this.albums[this.currentIndex].color;
    return {
      'background': `linear-gradient(45deg, ${color}, #121212)`
    };
  }

  getBlurBackgroundStyle(): any {
    const color = this.albums[this.currentIndex].color;
    return {
      'background': color
    };
  }

  getAlbumClass(index: number): string {
    // Calculate relative position (circular)
    const position = (index - this.currentIndex + this.albums.length) % this.albums.length;
    
    // Exactly 3 albums are visible at any time - active, prev, and next
    if (position === 0) {
      return 'album active';
    } else if (position === 1) {
      return 'album next';
    } else if (position === this.albums.length - 1) {
      return 'album prev';
    } else {
      // All other albums are hidden completely
      return 'album';
    }
  }

  login(): void {
    this.authService.login();
  }

  startListening(): void {
    // Redirect to login which will then redirect to dashboard after auth
    this.login();
  }

  private initBackgroundShapes(): void {
  }
}