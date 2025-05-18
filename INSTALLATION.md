# INSTALLATION GUIDE

This guide provides complete instructions for installing and running the Connect The Dots application.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) (for containerized installation)
- Alternatively, for local development:
  - Java 17 or higher
  - Node.js 20 or higher
  - PostgreSQL 14 or higher

## Option 1: Quick Start with Docker (Recommended)

Docker provides the easiest way to get the entire application running with minimal setup.

### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/SWE573.git
cd SWE573
```

### Step 2: Configure Environment Variables

```bash
# Copy the example environment file to the root of the project
cp env.example .env

# Edit the .env file to customize settings (optional)
# For a quick start, the default values will work fine
```

The `.env` file should be placed in the root directory of the project (same level as docker-compose.yml).

For detailed information about all configuration options, see [Configuration Documentation](./config/CONFIG_DOCS.md).

### Step 3: Start the Application

```bash
# Build and start all services
docker-compose up -d
```

### Step 4: Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **PGAdmin** (Database Admin): http://localhost:5050
  - Email: admin@admin.com
  - Password: admin

The database will be automatically created and initialized on first startup.

## Option 2: Local Development Setup

For development purposes, you may want to run components individually.

### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/SWE573.git
cd SWE573
```

### Step 2: Set Up PostgreSQL Database

```bash
# Create a new database
createdb connect_the_dots_db

# Or use an existing PostgreSQL installation with the following details:
# Database: connect_the_dots_db
# Username: postgres
# Password: (your password)
```

### Step 3: Configure and Run the Backend

```bash
# Navigate to backend directory
cd backend

# Create application.properties file in src/main/resources
mkdir -p src/main/resources
cat > src/main/resources/application.properties << EOF
spring.datasource.url=jdbc:postgresql://localhost:5432/connect_the_dots_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
jwt.secret=your_secure_jwt_secret_key_at_least_32_characters
jwt.expiration=86400000
logging.level.org.springframework=INFO
logging.level.com.swe573=DEBUG
server.port=8080
server.servlet.context-path=/api
EOF

# Run the backend application
./mvnw spring-boot:run
```

The backend will be available at http://localhost:8080/api

### Step 4: Configure and Run the Frontend

In a new terminal window:

```bash
# Navigate to frontend directory
cd frontend/threadapp

# Install dependencies
npm install

# Create environment configuration file .env.local in the frontend/threadapp directory
cat > .env.local << EOF
VITE_API_BASE_URL=http://localhost:8080/api
EOF

# Start the development server
npm run dev
```

The frontend will be available at http://localhost:5173

## Configuration Files Summary

| File | Path | Purpose | Required to create manually? |
|------|------|---------|------------------------------|
| `.env` | `./SWE573/.env` | Docker environment variables | Yes - for Docker deployment |
| `application.properties` | `./SWE573/backend/src/main/resources/application.properties` | Backend settings | Yes - for local backend development |
| `.env.local` | `./SWE573/frontend/threadapp/.env.local` | Frontend environment variables | Yes - for local frontend development |
| `nginx.conf` | `./SWE573/frontend/threadapp/nginx.conf` | Frontend web server config | No - created automatically by Docker |

**Note**: You only need to manually create the configuration files relevant to your deployment method. For Docker deployment, you only need to create the `.env` file. The nginx configuration is automatically handled by the Docker build process.

For more details about configuration options, see the [Configuration Documentation](./config/CONFIG_DOCS.md).

## Verifying Installation

### Test Backend API

```bash
# Check if the backend is running
curl http://localhost:8080/api/health

# Expected response: "Service is running"
```

### Test Frontend

1. Open a web browser and navigate to:
   - http://localhost:3000 (for Docker installation)
   - http://localhost:5173 (for local development)

2. You should see the Connect The Dots login page.

### Test Database Connection

If using Docker:
1. Access PGAdmin at http://localhost:5050
2. Login with admin@admin.com / admin
3. Add a new server:
   - Name: SWE573
   - Host: postgres
   - Port: 5432
   - Database: connect_the_dots_db
   - Username: postgres
   - Password: swe573 (or what you set in .env)

## Troubleshooting

### Docker Issues

```bash
# Check container status
docker-compose ps

# View logs from all containers
docker-compose logs

# View logs from a specific service
docker-compose logs backend
docker-compose logs frontend
docker-compose logs postgres
```

### Database Connection Issues

- Ensure PostgreSQL is running
- Verify the database credentials match in your configuration
- Check for any firewall issues blocking port 5432

### Frontend Not Connecting to Backend

- Verify the `VITE_API_BASE_URL` points to the correct backend URL
- Check for CORS issues in the browser console
- Ensure the backend is running and accessible

### Backend Not Starting

- Check for port conflicts on 8080
- Ensure Java 17+ is installed (for local development)
- Verify database connection settings

## Next Steps

After successful installation:

1. Create an admin user through the registration page
2. Begin setting up your knowledge graph threads
3. Explore the application features as described in the User Guide

## Database Initialization

The application uses JPA/Hibernate with `spring.jpa.hibernate.ddl-auto=update` which means:

1. On first startup, all database tables will be created automatically
2. On subsequent startups, the schema will be updated if entity classes change
3. Your data will be preserved between restarts

No manual database initialization is needed. 