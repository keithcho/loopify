# Loopify - AI-Powered Music Recommendation App

Loopify is an advanced music recommendation application that leverages AI to analyze your Spotify playlists and listening habits to generate personalized song recommendations. Featuring seamless Spotify integration, an interactive music player with preview capabilities, and customizable recommendation prompts, Loopify helps you discover new music that truly aligns with your unique taste.

![Loopify](https://via.placeholder.com/800x400)

## Table of Contents
- [Features](#features)
- [Architecture Overview](#architecture-overview)
- [Technologies Used](#technologies-used)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Environment Setup](#environment-setup)
  - [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core Features
- **Spotify Authentication**: Secure OAuth 2.0 integration with Spotify API
- **AI-Powered Recommendations**: Uses Google's Gemini AI to analyze your music taste and generate recommendations
- **Customizable Recommendation Prompts**: Influence recommendations with custom instructions (e.g., "more upbeat songs" or "similar to jazz")
- **Playlist-Based Recommendations**: Get suggestions based on any of your Spotify playlists
- **Top Tracks Analysis**: Get recommendations based on your most-played tracks across different time periods
- **Music Preview Player**: Listen to 30-second previews directly in the application
- **Add to Playlist**: Easily add recommended tracks to your Spotify playlists

### Advanced Features
- **Cached Recommendations**: Redis caching system for faster load times and reduced API calls
- **Continuous Scrolling**: Automatic loading of additional recommendations as you explore
- **Fallback System**: Alternative recommendation engine if AI service is unavailable
- **Performance Monitoring**: Prometheus and Grafana integration for real-time system metrics
- **Progressive Web App**: Installable on mobile devices with offline capabilities

## Architecture Overview

Loopify follows a modern microservices architecture with:

- **Frontend**: Angular-based SPA with responsive design using Tailwind CSS
- **Backend**: Spring Boot Java application handling business logic and API integration
- **Databases**: MySQL for user data and Redis for caching
- **AI Integration**: Google Gemini API for intelligent music analysis
- **Monitoring**: Prometheus and Grafana for system metrics

The application uses a containerized approach with Docker and Docker Compose for easy deployment and scaling.

## Technologies Used

### Frontend
- Angular 19
- TypeScript
- Tailwind CSS
- RxJS
- Progressive Web App (PWA) capabilities

### Backend
- Java 23
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Redis

### Databases
- MySQL 8.0
- Redis 7.0

### DevOps & Monitoring
- Docker & Docker Compose
- Prometheus
- Grafana
- Micrometer

### External Services
- Spotify Web API
- Google Gemini API (for AI recommendations)

## Project Structure

```
loopify/
├── client/                      # Angular frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/      # Angular components
│   │   │   ├── models/          # TypeScript interfaces
│   │   │   ├── services/        # API services
│   │   │   └── store/           # State management
│   │   ├── assets/              # Static assets
│   │   └── environments/        # Environment configurations
│   ├── angular.json             # Angular configuration
│   └── package.json             # Frontend dependencies
├── server/                      # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/vttp/final_project/
│   │   │   │   ├── configurations/  # Spring configurations
│   │   │   │   ├── controllers/     # REST controllers
│   │   │   │   ├── models/          # Data models
│   │   │   │   ├── repositories/    # Data access
│   │   │   │   ├── services/        # Business logic
│   │   │   │   └── utils/           # Utility classes
│   │   │   └── resources/           # Application properties
│   │   └── test/                    # Unit and integration tests
│   └── pom.xml                      # Backend dependencies
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Docker build instructions
└── README.md                    # This file
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Java Development Kit (JDK) 17+
- Docker and Docker Compose
- Spotify Developer Account (for API credentials)
- Google AI Studio Account (for Gemini API access)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/loopify.git
   cd loopify
   ```

2. Install frontend dependencies:
   ```bash
   cd client
   npm install
   cd ..
   ```

3. Install backend dependencies:
   ```bash
   cd server
   ./mvnw clean install
   cd ..
   ```

### Environment Setup

1. Create a `.env` file in the root directory with the following variables:

```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=loopify_user
MYSQL_PASSWORD=your_password

# Redis Configuration
REDIS_PASSWORD=your_redis_password

# Spotify API Credentials
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret

# Encryption Secret
APP_ENCRYPTION_SECRET=your_encryption_secret

# Gemini API
GEMINI_API_KEY=your_gemini_api_key

# Grafana (Optional)
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
```

2. Create a `secrets.properties` file in `server/src/main/resources/` with:

```properties
# Spotify Configuration
spotify.client.id=${SPOTIFY_CLIENT_ID}
spotify.client.secret=${SPOTIFY_CLIENT_SECRET}

# Encryption Secret
app.encryption.secret=${APP_ENCRYPTION_SECRET}

# Gemini API Key
gemini.api-key=${GEMINI_API_KEY}
```

### Running the Application

#### Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start all required services:
- Loopify application (frontend and backend): http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

#### Running Separately for Development

1. Start the backend:
   ```bash
   cd server
   ./mvnw spring-boot:run
   ```

2. Start the frontend:
   ```bash
   cd client
   ng serve --proxy-config proxy.conf.json
   ```

3. Access the application at http://localhost:4200

## Usage Guide

### Authentication
1. Visit the landing page and click "Login with Spotify"
2. Authorize the application to access your Spotify account
3. You'll be redirected to the dashboard upon successful authentication

### Getting Recommendations from Playlists
1. Navigate to "Your Playlists" from the dashboard
2. Browse or search for a specific playlist
3. Click "Select" on your chosen playlist
4. Loopify will analyze your playlist and generate recommendations
5. Customize recommendations using the prompt field (e.g., "more upbeat songs")

### Getting Recommendations from Top Tracks
1. Click on "From Your Top Tracks" on the dashboard
2. Select a time range (Last 4 Weeks, Last 6 Months, or All Time)
3. View your top tracks and click "Get Recommendations"
4. Explore recommendations based on your most listened tracks

### Music Player
1. Navigate through recommendations using the carousel 
2. Click the play button to listen to a 30-second preview
3. Adjust volume using the volume slider
4. Add songs you like to your Spotify playlist with the "+" button

## API Documentation

### Authentication Endpoints
- `GET /api/auth/spotify`: Initiates Spotify OAuth flow
- `GET /api/auth/spotify/callback`: Handles Spotify OAuth callback
- `GET /api/auth/spotify/logout`: Logs out the current user

### Spotify API Endpoints
- `GET /api/spotify/profile`: Get current user's Spotify profile
- `GET /api/spotify/playlists`: Get user's playlists with pagination
- `GET /api/spotify/playlists/{id}`: Get a specific playlist by ID
- `GET /api/spotify/top-tracks`: Get user's top tracks with time range options
- `POST /api/spotify/playlists/{id}/tracks`: Add track to a playlist

### Recommendation Endpoints
- `GET /api/gemini/recommendations`: Get AI recommendations based on a playlist
- `GET /api/gemini/top-tracks-recommendations`: Get recommendations based on top tracks

### Preview Endpoints
- `GET /api/spotify/preview`: Get track preview URL
- `POST /api/spotify/preview/batch`: Get multiple preview URLs

## Deployment

### Production Deployment

For production deployment, we use Docker Compose with proper environment variables and security measures:

1. Set up your production environment variables in `.env`
2. Build the production image:
   ```bash
   docker build -t loopify:latest .
   ```
3. Run with Docker Compose:
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

## Monitoring

Loopify includes comprehensive monitoring capabilities:

- **Prometheus**: Collects metrics from the application
- **Grafana**: Visualizes metrics with pre-configured dashboards
- **Micrometer**: Provides metrics collection within the application

Key metrics tracked:
- API call counts and response times
- User session counts
- Recommendation generation times
- Cache hit/miss ratios
- System resource usage

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Acknowledgements
- [Spotify Web API](https://developer.spotify.com/documentation/web-api/) for music data
- [Google Gemini API](https://ai.google.dev/) for AI recommendations