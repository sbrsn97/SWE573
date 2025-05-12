# Deployment Guide

This guide provides step-by-step instructions for deploying the Connect The Dots application in various environments.

## Table of Contents

1. [Local Development Deployment](#local-development-deployment)
2. [Docker Deployment](#docker-deployment)
3. [Production Deployment](#production-deployment)
4. [Database Management](#database-management)
5. [Monitoring and Maintenance](#monitoring-and-maintenance)

## Local Development Deployment

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- PostgreSQL 14 or higher
- Maven
- npm

### Backend Setup

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/SWE573.git
   cd SWE573
   ```

2. Configure database connection in `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/connect_the_dots_db
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```

3. Build and run the Spring Boot application:
   ```
   cd backend
   ./mvnw spring-boot:run
   ```
   The backend will be available at http://localhost:8080/api

### Frontend Setup

1. Navigate to the frontend directory:
   ```
   cd frontend/threadapp
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Configure API endpoint in `.env.local`:
   ```
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

4. Start the development server:
   ```
   npm run dev
   ```
   The frontend will be available at http://localhost:5173

## Docker Deployment

### Prerequisites

- Docker
- Docker Compose

### Deployment Steps

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/SWE573.git
   cd SWE573
   ```

2. Create a `.env` file based on the example:
   ```
   cp env.example .env
   ```

3. Modify the `.env` file with your configurations:
   ```
   POSTGRES_DB=connect_the_dots_db
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=your_secure_password
   JWT_SECRET=your_secure_jwt_secret
   ```

4. Build and start the Docker containers:
   ```
   docker-compose up -d
   ```

5. Access the application:
   - Frontend: http://localhost
   - Backend API: http://localhost:8080/api
   - PGAdmin: http://localhost:5050

### Managing Docker Deployments

#### Viewing Logs

```
docker-compose logs -f [service_name]
```

Replace `[service_name]` with `backend`, `frontend`, or `postgres`.

#### Restarting Services

```
docker-compose restart [service_name]
```

#### Stopping the Application

```
docker-compose down
```

To remove volumes (including database data):
```
docker-compose down -v
```

## Production Deployment

### Considerations for Production

1. Use environment-specific configurations
2. Secure sensitive information
3. Implement HTTPS
4. Set up proper logging and monitoring
5. Configure backups
6. Use container orchestration for scaling

### Server Prerequisites

- Linux server (Ubuntu 20.04 LTS recommended)
- Docker and Docker Compose
- Domain name with DNS configured
- SSL certificate

### Deployment Steps

1. Clone the repository on your production server:
   ```
   git clone https://github.com/yourusername/SWE573.git
   cd SWE573
   ```

2. Create a production `.env` file:
   ```
   cp env.example .env.prod
   ```

3. Configure production settings in `.env.prod`:
   ```
   POSTGRES_DB=connect_the_dots_db
   POSTGRES_USER=secure_db_user
   POSTGRES_PASSWORD=very_secure_password
   JWT_SECRET=long_random_secure_key
   ```

4. Set up HTTPS with Let's Encrypt:
   ```
   # Install certbot
   apt-get update
   apt-get install certbot
   
   # Generate certificate
   certbot certonly --standalone -d yourdomain.com
   ```

5. Create or modify `docker-compose.prod.yml` to include SSL certificates:
   ```yaml
   version: '3.8'
   
   services:
     frontend:
       # ... existing config ...
       volumes:
         - /etc/letsencrypt/live/yourdomain.com/fullchain.pem:/etc/nginx/certs/fullchain.pem
         - /etc/letsencrypt/live/yourdomain.com/privkey.pem:/etc/nginx/certs/privkey.pem
   ```

6. Update Nginx configuration to use SSL:
   ```
   # In frontend/threadapp/nginx.conf
   server {
       listen 443 ssl;
       ssl_certificate /etc/nginx/certs/fullchain.pem;
       ssl_certificate_key /etc/nginx/certs/privkey.pem;
       # ... rest of configuration ...
   }
   
   server {
       listen 80;
       return 301 https://$host$request_uri;
   }
   ```

7. Deploy with Docker Compose:
   ```
   docker-compose -f docker-compose.prod.yml up -d
   ```

8. Set up automatic SSL renewal:
   ```
   # Add to crontab
   0 3 * * * certbot renew --quiet && docker-compose -f /path/to/docker-compose.prod.yml restart frontend
   ```

## Database Management

### Initial Setup

With `spring.jpa.hibernate.ddl-auto=update`, JPA will automatically create and update the database schema. For the first run, ensure:

1. The database exists:
   ```sql
   CREATE DATABASE connect_the_dots_db;
   ```

2. The user has appropriate permissions:
   ```sql
   CREATE USER secure_db_user WITH PASSWORD 'very_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE connect_the_dots_db TO secure_db_user;
   ```

### Database Backups

1. Set up regular PostgreSQL backups:
   ```bash
   # Add to crontab
   0 2 * * * docker exec swe573-postgres pg_dump -U postgres connect_the_dots_db > /backup/connect_the_dots_$(date +\%Y\%m\%d).sql
   ```

2. Configure retention policy:
   ```bash
   # Add to crontab
   0 3 * * * find /backup -name "connect_the_dots_*.sql" -mtime +30 -delete
   ```

### Database Migration

For production, it's recommended to manage schema changes through migration scripts:

1. Use a tool like Flyway or Liquibase
2. Set `spring.jpa.hibernate.ddl-auto=validate` to prevent automatic schema changes
3. Create migration scripts for each schema change

## Monitoring and Maintenance

### Health Checks

Implement health check endpoints to monitor application status:

```java
@GetMapping("/health")
public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("Service is running");
}
```

### Log Management

1. Configure centralized logging with ELK Stack or similar
2. Set up log rotation:
   ```
   # In logrotate.d/docker
   /var/lib/docker/containers/*/*.log {
       rotate 7
       daily
       compress
       delaycompress
       missingok
       copytruncate
   }
   ```

### Resource Monitoring

1. Set up container resource monitoring:
   ```
   docker stats
   ```

2. Consider using monitoring tools like:
   - Prometheus and Grafana
   - Datadog
   - New Relic

### Regular Maintenance

1. Update dependencies regularly
2. Apply security patches promptly
3. Test backups periodically
4. Review logs for errors
5. Monitor performance metrics 