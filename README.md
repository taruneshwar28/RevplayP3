# RevPlay Microservices

A music streaming platform built with Spring Boot microservices architecture.

## Architecture

The platform is organized as Spring Boot microservices behind an API Gateway:

- API Gateway (`8080`)
- Auth Service (`8081`)
- User Service (`8082`)
- Artist Service (`8083`)
- Music Service (`8084`)
- Player Service (`8085`)
- Analytics Service (`8086`)

## Services

| Service | Port | Description | Owner |
|---------|------|-------------|-------|
| Config Server | 8888 | Centralized configuration | Shared |
| Discovery Server | 8761 | Eureka service registry | Shared |
| API Gateway | 8080 | Request routing and JWT validation | Shared |
| Auth Service | 8081 | Authentication and JWT tokens | Team |
| User Service | 8082 | User profiles, playlists, favorites | Team |
| Artist Service | 8083 | Artist profiles, songs, albums | Team |
| Music Service | 8084 | Music discovery and search | Team |
| Player Service | 8085 | Play music and listening history | Team |
| Analytics Service | 8086 | Artist analytics and insights | Team |

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker and Docker Compose (optional)

## Running Locally

### 1. Build all services

```bash
mvn clean package -DskipTests
```

### 2. Start services in order

```bash
cd config-server && mvn spring-boot:run
cd discovery-server && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
cd auth-service && mvn spring-boot:run
cd user-service && mvn spring-boot:run
cd artist-service && mvn spring-boot:run
cd music-service && mvn spring-boot:run
cd player-service && mvn spring-boot:run
cd analytics-service && mvn spring-boot:run
```

### Using Docker Compose

```bash
mvn clean package -DskipTests
docker-compose up --build
```

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`.

### Auth Service

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/validate`

### User Service

- `GET /api/users/profile`
- `PUT /api/users/profile`
- `GET /api/users/stats`
- `POST /api/playlists`
- `GET /api/playlists`
- `PUT /api/playlists/{id}`
- `DELETE /api/playlists/{id}`
- `POST /api/playlists/{id}/songs/{songId}`
- `DELETE /api/playlists/{id}/songs/{songId}`
- `POST /api/favorites/{songId}`
- `DELETE /api/favorites/{songId}`
- `GET /api/favorites`

### Artist Service

- `GET /api/artists/{artistId}`
- `PUT /api/artists/{artistId}`
- `POST /api/artists/{artistId}/songs`
- `GET /api/artists/{artistId}/songs`
- `PUT /api/artists/{artistId}/songs/{songId}`
- `DELETE /api/artists/{artistId}/songs/{songId}`
- `POST /api/artists/{artistId}/albums`
- `GET /api/artists/{artistId}/albums`
- `PUT /api/artists/{artistId}/albums/{albumId}`
- `DELETE /api/artists/{artistId}/albums/{albumId}`

### Music Service

- `GET /api/catalog/songs`
- `GET /api/catalog/songs/{songId}`
- `GET /api/catalog/songs/search`
- `GET /api/catalog/genres`
- `GET /api/catalog/trending`

### Player Service

- `POST /api/player/play`
- `GET /api/player/now-playing`
- `GET /api/history/recent`
- `DELETE /api/history`

### Analytics Service

- `GET /api/analytics/artist/{artistId}/overview`
- `GET /api/analytics/artist/{artistId}/songs`
- `GET /api/analytics/artist/{artistId}/top`
- `GET /api/analytics/artist/{artistId}/trends`
- `GET /api/analytics/artist/{artistId}/listeners`

## Technology Stack

- Spring Boot 3.2
- Spring Cloud
- Spring Security with JWT
- Spring Data JPA
- H2 Database for development
- OpenFeign
- Resilience4j
- Docker
