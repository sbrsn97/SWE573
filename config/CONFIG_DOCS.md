# Configuration Guide

This document provides detailed information about all configuration options for the Connect The Dots application.

## Configuration Files and Their Locations

| File | Path | Purpose | When to use |
|------|------|---------|-------------|
| `.env` | `/Users/username/SWE573/.env` | Main configuration file for Docker deployment | When using Docker Compose |
| `application.properties` | `/Users/username/SWE573/backend/src/main/resources/application.properties` | Backend configuration | When running backend locally |
| `.env.local` | `/Users/username/SWE573/frontend/threadapp/.env.local` | Frontend environment variables | When running frontend locally |
| `nginx.conf` | `/Users/username/SWE573/frontend/threadapp/nginx.conf` | Nginx web server configuration | **Automatically created by Docker - most users don't need to modify this** |

Replace `/Users/username/SWE573/` with the actual path where you cloned the repository.

## Using Configuration Files

### For Docker Deployment

1. Create `.env` in the root of the project:
   ```bash
   cd /path/to/SWE573
   cp env.example .env
   # Edit .env as needed
   ```

2. All other configuration files are used automatically by the Docker containers. The nginx configuration is handled automatically by the Docker build process, so you typically don't need to create or modify it.

### For Local Development

1. Backend configuration:
   ```bash
   cd /path/to/SWE573/backend
   # Create and edit application.properties
   vim src/main/resources/application.properties
   ```

2. Frontend configuration:
   ```bash
   cd /path/to/SWE573/frontend/threadapp
   # Create and edit .env.local
   vim .env.local
   ```

## Environment Variables

The application is primarily configured through environment variables. For Docker Compose deployments, these are set in the `.env` file.

### Database Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `POSTGRES_DB` | PostgreSQL database name | connect_the_dots_db | connect_the_dots_db |
| `POSTGRES_USER` | PostgreSQL username | postgres | db_user |
| `POSTGRES_PASSWORD` | PostgreSQL password | *None* | secure_password123 |

### JWT Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `JWT_SECRET` | Secret key for JWT token generation | *None* | a7dh3k9fj20sla8dhf92jd8f7s0d9f87 |
| `JWT_EXPIRATION` | Token expiration time in milliseconds | 86400000 (24 hours) | 3600000 (1 hour) |

### PGAdmin Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `PGADMIN_DEFAULT_EMAIL` | Default admin email for PGAdmin | admin@admin.com | admin@example.com |
| `PGADMIN_DEFAULT_PASSWORD` | Default admin password for PGAdmin | admin | secure_admin_password |

### Frontend Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `VITE_API_BASE_URL` | URL for the backend API | http://localhost:8080/api | https://api.example.com |

## Spring Boot Configuration (application.properties)

### Database Connection

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/connect_the_dots_db
spring.datasource.username=postgres
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### JPA/Hibernate Configuration

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

The `spring.jpa.hibernate.ddl-auto` property has several possible values:
- `update`: Updates the schema if necessary (recommended for development)
- `create`: Creates the schema, destroying previous data
- `create-drop`: Creates the schema and drops it when the application stops
- `validate`: Validates the schema but makes no changes
- `none`: Disables DDL handling (recommended for production)

### JWT Configuration

```properties
jwt.secret=your_secure_jwt_secret_key_at_least_32_characters
jwt.expiration=86400000
```

### Logging Configuration

```properties
logging.level.org.springframework=INFO
logging.level.com.swe573=DEBUG
```

### Server Configuration

```properties
server.port=8080
server.servlet.context-path=/api
```

## Nginx Configuration

The Nginx configuration controls how the frontend is served and how API requests are proxied to the backend.

### Basic Configuration

```nginx
server {
    listen 80;
    server_name your_domain.com;
    
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

### API Proxy Configuration

```nginx
location /api {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### WebSocket Configuration

```nginx
location /ws {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

## Docker Compose Configuration

Docker Compose allows you to run multi-container applications. The `docker-compose.yml` file defines these services.

### PostgreSQL Configuration

```yaml
postgres:
  image: postgres:14
  environment:
    POSTGRES_DB: ${POSTGRES_DB}
    POSTGRES_USER: ${POSTGRES_USER}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  volumes:
    - postgres_data:/var/lib/postgresql/data
```

### Backend Configuration

```yaml
backend:
  build: 
    context: ./backend
    dockerfile: Dockerfile
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
    SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    SPRING_JPA_HIBERNATE_DDL_AUTO: update
    JWT_SECRET: ${JWT_SECRET}
```

### Frontend Configuration

```yaml
frontend:
  build:
    context: ./frontend/threadapp
    dockerfile: Dockerfile
  ports:
    - "80:80"
  depends_on:
    - backend
```

## Production Considerations

For production deployments, consider the following changes:

1. Set `spring.jpa.hibernate.ddl-auto=none` or `validate` to prevent schema changes
2. Use strong, unique passwords for all services
3. Enable HTTPS with a valid SSL certificate
4. Implement proper backup strategies for the database
5. Set up monitoring and logging solutions
6. Use environment-specific configuration files
7. Implement rate limiting and security headers in Nginx 