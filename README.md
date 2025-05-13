# GnGm - Multiplayer Top-Down Shooter Game

A multiplayer top-down shooter game inspired by Hotline Miami's visual style, featuring an arms race (gun game) mechanic.

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- Spring WebSocket
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Maven

### Frontend
- HTML5 Canvas
- Phaser.js
- WebSocket

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Redis
- Node.js (for frontend development)

## Project Structure

```
gngm-game/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── gngm/
│   │   │           ├── config/           # Configuration classes
│   │   │           ├── controller/       # REST controllers
│   │   │           ├── dto/             # Data Transfer Objects
│   │   │           ├── entity/          # JPA entities
│   │   │           ├── mapper/          # MapStruct mappers
│   │   │           ├── repository/      # JPA repositories
│   │   │           ├── service/         # Business logic
│   │   │           ├── websocket/       # WebSocket handlers
│   │   │           └── GnGmApplication.java
│   │   └── resources/
│   │       ├── static/                  # Frontend assets
│   │       ├── templates/               # HTML templates
│   │       └── application.properties
│   └── test/                           # Test classes
└── pom.xml
```

## Setup Instructions

1. Clone the repository
2. Create a PostgreSQL database named `gngm_db`
3. Update `application.properties` with your database credentials
4. Start Redis server
5. Build the project:
   ```bash
   mvn clean install
   ```
6. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Development Phases

### Phase 1: Core Mechanics
- [ ] Single-player movement and shooting mechanics
- [ ] Basic map rendering and collision
- [ ] Weapon progression system

### Phase 2: Multiplayer Foundation
- [ ] WebSocket implementation
- [ ] Player synchronization
- [ ] Basic lobby system

### Phase 3: Game Features
- [ ] Complete weapon set implementation
- [ ] Multiple maps
- [ ] Scoreboard and match statistics
- [ ] Sound effects

### Phase 4: Polish
- [ ] UI improvements
- [ ] Player customization
- [ ] Performance optimization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 