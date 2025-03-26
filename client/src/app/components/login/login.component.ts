import { Component, inject, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  error: string | null = null;
  logoutMessage: string | null = null;

  private authService = inject(AuthService)
  private route = inject(ActivatedRoute)
  private router = inject(Router)

  ngOnInit(): void {
    // Check for error or logout parameters
    this.route.queryParams.subscribe(params => {
      if (params['error']) {
        this.error = this.getErrorMessage(params['error']);
      }
      
      if (params['logout'] === 'success') {
        this.logoutMessage = 'You have been successfully logged out.';
      }
    });
    
    // If already logged in, redirect to dashboard
    this.authService.isAuthenticated$.subscribe(isAuthenticated => {
      if (isAuthenticated) {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  login(): void {
    this.authService.login();
  }
  
  private getErrorMessage(errorCode: string): string {
    switch (errorCode) {
      case 'session_expired':
        return 'Your session has expired. Please try logging in again.';
      case 'state_mismatch':
        return 'Security verification failed. Please try again.';
      case 'missing_verifier':
        return 'Authentication process incomplete. Please try again.';
      case 'token_exchange_failed':
        return 'Failed to complete authentication. Please try again.';
      case 'access_denied':
        return 'You declined to authorize the application. Please try again.';
      default:
        return `Authentication error: ${errorCode}`;
    }
  }
}
