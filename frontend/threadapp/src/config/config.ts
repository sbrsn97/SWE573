export const API_BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
    auth: {
        register: `${API_BASE_URL}/auth/register`,
        login: `${API_BASE_URL}/auth/login`,
        logout: `${API_BASE_URL}/auth/logout`
    },
    users: {
        me: `${API_BASE_URL}/users/me`,
        all: `${API_BASE_URL}/users`
    }
}; 