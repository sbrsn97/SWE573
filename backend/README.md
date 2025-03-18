# SWE573 Backend

This is the backend service for the SWE573 project, built with Spring Boot.

## Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL

## Setup

1. Install PostgreSQL and create a database named `swe573`
2. Update database credentials in `src/main/resources/application.properties` if needed
3. Build the project:
   ```bash
   mvn clean install
   ```

## Running the Application

You can run the application using Maven:

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── swe573/
│   │   │           └── Application.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       └── resources/
└── pom.xml
```

## Development

- The project uses Spring Boot 3.2.3
- JPA for database operations
- Lombok for reducing boilerplate code
- PostgreSQL as the database 