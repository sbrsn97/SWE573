# Connect The Dots - SWE573 Project

A web application that allows users to create, share, and explore knowledge graphs, built with Spring Boot and React.

## Project Structure

```
.
├── backend/                   # Spring Boot backend
│   ├── src/                   # Source code
│   │   ├── main/
│   │   │   ├── java/com/swe573/        # Java code
│   │   │   │   ├── controllers/        # REST API endpoints
│   │   │   │   ├── models/             # Data models
│   │   │   │   ├── repositories/       # Database access
│   │   │   │   ├── services/           # Business logic
│   │   │   │   ├── dto/                # Data transfer objects
│   │   │   │   └── config/             # Configuration
│   │   │   └── resources/             # Application properties, static resources
│   │   └── test/                      # Test code
│   ├── pom.xml                        # Maven dependencies
│   └── Dockerfile                     # Docker configuration for backend
├── frontend/                          # React frontend
│   └── threadapp/                     # React application
│       ├── src/                       # Source code
│       │   ├── components/            # React components
│       │   ├── services/              # API services
│       │   ├── pages/                 # Page components
│       │   ├── utils/                 # Utility functions
│       │   └── config/                # Configuration
│       ├── public/                    # Static assets
│       ├── package.json               # npm dependencies
│       └── Dockerfile                 # Docker configuration for frontend
├── docker-compose.yml                 # Docker Compose configuration
├── env.example                        # Example environment variables
└── config/                            # Configuration samples
    ├── application.properties.sample  # Backend properties sample
    └── nginx.conf.sample              # Nginx configuration sample
```

## Features

- **User Management**: Registration, login, profile management
- **Thread Creation**: Create and share threads with knowledge graphs
- **Graph Editor**: Visual graph editor to create connections between knowledge nodes
- **Commenting and Voting**: Engage with threads through comments and upvotes
- **Notifications**: Real-time notifications for user interactions
- **Search Functionality**: Search threads, users, and graph content
- **Analytics**: View trending and popular threads

## Technologies Used

- **Backend**: Spring Boot, PostgreSQL, JPA/Hibernate, JWT Authentication
- **Frontend**: React, TypeScript, PrimeReact, TailwindCSS, Vite
- **Deployment**: Docker, Nginx
- **Tools**: Maven, npm

## Prerequisites

- Docker and Docker Compose
- Java 17 (for local development)
- Node.js 18+ (for local development)

## Running the Application with Docker

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/SWE573.git
   cd SWE573
   ```

2. Create a `.env` file based on the example:
   ```
   cp env.example .env
   ```
   
3. Update the `.env` file with your configuration values.

4. Build and start the containers:
   ```
   docker-compose up -d
   ```

5. The application will be available at:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080/api
   - PGAdmin: http://localhost:5050 (admin@admin.com / admin)

### Running Individual Components

#### Backend (Spring Boot)

1. Navigate to the backend directory:
   ```
   cd backend
   ```

2. Build and run with Maven:
   ```
   ./mvnw spring-boot:run
   ```

#### Frontend (React)

1. Navigate to the frontend directory:
   ```
   cd frontend/threadapp
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Make sure the `.env` file is configured to use the correct API endpoint:
   ```
   echo "VITE_API_BASE_URL=http://localhost:8080/api" > .env.local
   ```

4. Start the development server:
   ```
   npm run dev
   ```

## Configuration

### Environment Variables

All configuration is controlled through environment variables set in the `.env` file. Key configurations include:

#### Database Settings
- `POSTGRES_DB`: Database name
- `POSTGRES_USER`: Database username
- `POSTGRES_PASSWORD`: Database password

#### JWT Authentication
- `JWT_SECRET`: Secret key for JWT token generation
- `JWT_EXPIRATION`: Token expiration time in milliseconds

#### Frontend Settings
- `VITE_API_BASE_URL`: URL for the backend API

See `env.example` for a complete list of available options.

### Frontend Environment Variables

The frontend uses Vite's environment variable system. You can configure the API endpoint and other variables by:

1. Creating a `.env.local` file in the `frontend/threadapp` directory:
   ```
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

2. For production, set the environment variables in the Docker Compose file:
   ```yaml
   frontend:
     build:
       context: ./frontend/threadapp
     environment:
       - VITE_API_BASE_URL=http://api.example.com
   ```

3. For different environments, you can use `.env.production`, `.env.development`, etc.

Environment variables are accessed in the code using `import.meta.env.VITE_API_BASE_URL`.

### Backend Configuration

The backend application is configured through `application.properties`. Important settings include:

- Database connection parameters
- JPA/Hibernate properties
- JWT token configuration
- Logging settings

See `config/application.properties.sample` for details.

## Database Initialization

JPA/Hibernate will automatically create the database schema on first run when using the `spring.jpa.hibernate.ddl-auto=update` setting. This means you don't need to manually initialize the database.

## Deployment

For production deployment:

1. Update the `.env` file with production-suitable values:
   - Use strong passwords for all services
   - Set a secure JWT secret
   - Configure domain names

2. Build and deploy using Docker Compose:
   ```
   docker-compose -f docker-compose.yml up -d
   ```

For SSL/TLS support, consider using a reverse proxy like Nginx with Let's Encrypt for SSL certificates.

## Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running and accessible
- Verify database credentials in the environment/configuration files
- Check that the database has been created

### Frontend Not Connecting to Backend
- Verify the `VITE_API_BASE_URL` setting points to the correct backend URL
- Check CORS configuration in the backend

### Docker Issues
- Ensure Docker and Docker Compose are properly installed
- Check container logs: `docker-compose logs [service_name]`
- Verify port mappings match your environment

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- Spring Boot team for the excellent backend framework
- React team for the frontend library
- PrimeReact for UI components
- All contributors and testers 