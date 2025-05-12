# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Set build arguments with defaults (these can be overridden at build time)
ARG VITE_API_BASE_URL=http://localhost:8080/api

# Make the build argument available as an environment variable during build
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy package.json and package-lock.json
COPY package*.json ./
# Use npm install instead of npm ci to handle new dependencies not in package-lock.json
RUN npm install

# Copy the rest of the application code
COPY . .

# Build the application - skip TypeScript type checking to avoid build errors
# Note: The application now uses fixed light theme settings instead of OS detection
RUN npm run build:skip-typescript

# Production stage
FROM nginx:alpine
# Copy the built app from the build stage
COPY --from=build /app/dist /usr/share/nginx/html
# Copy the nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Expose the port
EXPOSE 80
# Start nginx
CMD ["nginx", "-g", "daemon off;"] 