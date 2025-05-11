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
        search: `${API_BASE_URL}/users/search`,
        follow: (id: number, followedId: number) => `${API_BASE_URL}/users/${id}/follow/${followedId}`,
        unfollow: (id: number, followedId: number) => `${API_BASE_URL}/users/${id}/unfollow/${followedId}`
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
        voteStatus: (id: number) => `${API_BASE_URL}/votes/thread/${id}/status`,
        preview: `${API_BASE_URL}/threads/preview`,
        following: `${API_BASE_URL}/threads/following`
    },
    comments: {
        create: `${API_BASE_URL}/comments`,
        getByThread: (threadId: number) => `${API_BASE_URL}/comments/thread/${threadId}`,
        getParentsByThread: (threadId: number) => `${API_BASE_URL}/comments/thread/${threadId}/parents`,
        getChildrenByParent: (parentId: number) => `${API_BASE_URL}/comments/${parentId}/children`,
        get: (id: number) => `${API_BASE_URL}/comments/${id}`,
        update: (id: number) => `${API_BASE_URL}/comments/${id}`,
        delete: (id: number) => `${API_BASE_URL}/comments/${id}`,
        hardDelete: (id: number) => `${API_BASE_URL}/comments/${id}/hard`,
        reactivate: (id: number) => `${API_BASE_URL}/comments/${id}/reactivate`
    },
    votes: {
        commentVote: (commentId: number) => `${API_BASE_URL}/votes/comment/${commentId}`,
        commentVoteStatus: (commentId: number) => `${API_BASE_URL}/votes/comment/${commentId}/status`,
        removeCommentVote: (commentId: number) => `${API_BASE_URL}/votes/comment/${commentId}`,
        threadVote: (threadId: number) => `${API_BASE_URL}/votes/thread/${threadId}`,
        threadVoteStatus: (threadId: number) => `${API_BASE_URL}/votes/thread/${threadId}/status`,
        removeThreadVote: (threadId: number) => `${API_BASE_URL}/votes/thread/${threadId}`,
        recalculateThreadVotes: (threadId: number) => `${API_BASE_URL}/votes/thread/${threadId}/recalculate`,
        resetAllVoteCounts: `${API_BASE_URL}/votes/reset-all-counts`,
        zeroAllVoteCounts: `${API_BASE_URL}/votes/zero-all-counts`
    },
    analytics: {
        hotThreads: (daysBack: number = 7, limit: number = 5) => 
            `${API_BASE_URL}/analytics/hot-threads?daysBack=${daysBack}&limit=${limit}`,
        recommendedThreads: (limit: number = 5) => 
            `${API_BASE_URL}/analytics/recommended-threads?limit=${limit}`,
        similarThreads: (threadId: number, limit: number = 5) => 
            `${API_BASE_URL}/analytics/similar-threads/${threadId}?limit=${limit}`,
        mostVoted: (limit: number = 5) => 
            `${API_BASE_URL}/analytics/most-voted?limit=${limit}`
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
    },
    graph: {
        // Node endpoints
        nodes: {
            create: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/nodes`,
            createBatch: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/nodes/batch`,
            update: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}`,
            delete: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}`,
            get: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}`,
            getByThread: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/nodes`,
            search: (threadId: number, query: string) => 
                `${API_BASE_URL}/graph/threads/${threadId}/nodes/search?query=${encodeURIComponent(query)}`
        },
        // Node details endpoints
        nodeDetails: {
            get: (detailsId: number) => `${API_BASE_URL}/graph/nodedetails/${detailsId}`,
            getByNode: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}/details`
        },
        // Edge endpoints
        edges: {
            create: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/edges`,
            createBatch: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/edges/batch`,
            update: (edgeId: number) => `${API_BASE_URL}/graph/edges/${edgeId}`,
            delete: (edgeId: number) => `${API_BASE_URL}/graph/edges/${edgeId}`,
            getByThread: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/edges`,
            getByNode: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}/edges`,
            search: (threadId: number, query: string) => 
                `${API_BASE_URL}/graph/threads/${threadId}/edges/search?query=${encodeURIComponent(query)}`
        },
        // Connection endpoints
        connections: {
            connect: (sourceNodeId: number, targetNodeId: number) => 
                `${API_BASE_URL}/graph/nodes/${sourceNodeId}/connect/${targetNodeId}`,
            disconnect: (sourceNodeId: number, targetNodeId: number) => 
                `${API_BASE_URL}/graph/nodes/${sourceNodeId}/disconnect/${targetNodeId}`,
            getConnections: (nodeId: number) => 
                `${API_BASE_URL}/graph/nodes/${nodeId}/connections`
        },
        // Analysis endpoints
        analysis: {
            getGraphAnalysis: (threadId: number) => `${API_BASE_URL}/graph/threads/${threadId}/analysis`,
            getConnectedNodes: (nodeId: number) => `${API_BASE_URL}/graph/nodes/${nodeId}/connections`
        }
    }
}; 