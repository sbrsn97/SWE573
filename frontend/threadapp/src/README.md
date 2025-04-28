# Authentication Architecture

This document outlines the authentication architecture implemented in the ThreadApp application.

## Overview

The application implements a comprehensive authentication system that:
1. Protects routes that require authentication
2. Handles token expiration globally
3. Redirects users to the login page when their token expires
4. Provides utility functions for making authenticated API requests

## Key Components

### 1. Auth Utilities (`/src/utils/authUtils.ts`)

This file contains the core authentication utilities:

- `handleAuthError`: Handles authentication errors, redirects to login page when token is expired
- `isAuthenticated`: Checks if the user has a valid token 
- `fetchWithAuth`: A wrapper around fetch that automatically adds the authentication token
- `setupAuthInterceptor`: Sets up a global interceptor for all fetch requests to handle token expiration

### 2. AuthRoute Component (`/src/components/auth/AuthRoute.tsx`) 

A wrapper component that protects routes requiring authentication. If a user tries to access a protected route without being authenticated, they will be redirected to the login page.

### 3. Route Protection (in `main.tsx`)

All routes that require authentication are wrapped with the `AuthRoute` component.

## How to Use

### 1. Making Authenticated API Requests

Instead of manually adding the token to each fetch request:

```typescript
// Don't do this
const token = localStorage.getItem('token');
if (!token) {
  navigate('/auth');
  return;
}

const response = await fetch(url, {
  headers: {
    'Authorization': `Bearer ${token}`,
    // other headers
  }
  // other options
});
```

Use the `fetchWithAuth` utility:

```typescript
// Do this instead
const response = await fetchWithAuth(url, {
  headers: {
    // other headers (no need to add Authorization)
  }
  // other options
});
```

### 2. Handling Authentication Errors

When making API requests, handle authentication errors using the `handleAuthError` utility:

```typescript
const response = await fetchWithAuth(url, options);

if (!response.ok) {
  // This will handle token expiration and redirect to login if needed
  if (handleAuthError(response, navigate)) return;
  
  // Handle other errors
  const errorData = await response.json();
  setError(errorData.message || `Error ${response.status}`);
  return;
}
```

### 3. Protecting Routes

To protect a route that requires authentication, wrap it with the `AuthRoute` component in `main.tsx`:

```typescript
<Route path="/protected-route" element={
  <AuthRoute>
    <YourComponent />
  </AuthRoute>
} />
```

## Token Expiration

When a token expires:

1. The global fetch interceptor detects the 401 response with a 'TOKEN_EXPIRED' code
2. It removes the token from localStorage
3. It redirects the user to the login page
4. After logging in again, the user will receive a new token

## Future Improvements

In the future, we could implement:

1. Token refresh functionality to extend the token lifetime
2. Remember the last route before authentication expired to redirect back after login
3. More sophisticated token validation including JWT decoding and expiration checking 