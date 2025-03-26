import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { BehaviorSubject, catchError, Observable, tap, throwError } from "rxjs";
import { environment } from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private apiUrl = environment.apiUrl;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.checkInitialAuthStatus());
  
  private http = inject(HttpClient)

  constructor() {
    // Check authentication status when service is initialized
    this.checkAuthStatus().subscribe();
  }
  
  // Getter for auth status as an observable
  get isAuthenticated$(): Observable<boolean> {
    return this.isAuthenticatedSubject.asObservable();
  }
  
  // Get current auth status
  get isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }
  
  // Initial auth check based on local data
  private checkInitialAuthStatus(): boolean {
    // We don't have a token to check locally since everything is managed by the backend session
    // This is just a placeholder that will be updated soon by the checkAuthStatus call
    return false;
  }
  
  // Check auth status with backend
  checkAuthStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/spotify/profile`, { withCredentials: true })
      .pipe(
        tap(() => this.isAuthenticatedSubject.next(true)),
        catchError(error => {
          this.isAuthenticatedSubject.next(false);
          return throwError(() => error);
        })
      );
  }
  
  // Initiate login process
  login(): void {
    // Redirect to backend auth endpoint which will handle Spotify OAuth flow
    window.location.href = `${this.apiUrl}/auth/spotify`;
  }
  
  // Logout user
  logout(): void {
    // Immediately update local state to prevent redirects
    this.isAuthenticatedSubject.next(false);
    
    // Clear any local storage if present
    localStorage.removeItem('spotify_auth');
    sessionStorage.removeItem('spotify_auth');
    
    // Then redirect to server logout endpoint
    window.location.href = `${this.apiUrl}/auth/spotify/logout?redirect=/landing`;
  }
}