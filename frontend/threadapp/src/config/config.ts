export const API_BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
    auth: {
        register: `${API_BASE_URL}/auth/register`,
        login: `${API_BASE_URL}/auth/login`,
        logout: `${API_BASE_URL}/auth/logout`
    },
    users: {
        me: `${API_BASE_URL}/users/me`,
        all: `${API_BASE_URL}/users`,
        update: (id: number) => `${API_BASE_URL}/users/${id}`,
        search: `${API_BASE_URL}/users/search`
    },
    threads: {
        create: `${API_BASE_URL}/threads`,
        get: (id: number) => `${API_BASE_URL}/threads/${id}`,
        getAll: `${API_BASE_URL}/threads`,
        search: `${API_BASE_URL}/threads/search`,
        update: (id: number) => `${API_BASE_URL}/threads/${id}`,
        delete: (id: number) => `${API_BASE_URL}/threads/${id}`,
        follow: (id: number) => `${API_BASE_URL}/threads/${id}/follow`,
        unfollow: (id: number) => `${API_BASE_URL}/threads/${id}/unfollow`,
        vote: (id: number) => `${API_BASE_URL}/threads/${id}/vote`,
        preview: `${API_BASE_URL}/threads/preview`
    },
    tags: {
        create: `${API_BASE_URL}/tags`,
        getAll: `${API_BASE_URL}/tags`,
        search: `${API_BASE_URL}/tags/search`,
        getByLabel: (label: string) => `${API_BASE_URL}/tags/label/${label}`,
        getByWikidata: (id: string) => `${API_BASE_URL}/tags/wikidata/${id}`
    },
    wikidata: {
        // Entity endpoints
        entities: {
            getAll: (page: number = 0, size: number = 10) => 
                `${API_BASE_URL}/wikidata/entities?page=${page}&size=${size}`,
            search: (query: string, page: number = 0, size: number = 10) => 
                `${API_BASE_URL}/wikidata/entities/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
            getById: (id: string) => 
                `${API_BASE_URL}/wikidata/entities/${id}`
        },
        // Topic endpoints
        topics: {
            getById: (id: string) => 
                `${API_BASE_URL}/wikidata/topics/${id}`,
            search: (query: string, page: number = 0, size: number = 10) => 
                `${API_BASE_URL}/wikidata/topics/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`
        },
        // Property endpoints
        properties: {
            getAll: (page: number = 0, size: number = 10) => 
                `${API_BASE_URL}/wikidata/properties?page=${page}&size=${size}`,
            search: (query: string, page: number = 0, size: number = 10) => 
                `${API_BASE_URL}/wikidata/properties/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
            getById: (id: string) => 
                `${API_BASE_URL}/wikidata/properties/${id}`
        }
    }
}; 