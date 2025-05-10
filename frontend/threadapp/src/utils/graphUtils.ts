import { API_ENDPOINTS } from '../config/config';
import { fetchWithAuth } from './authUtils';

/**
 * Interface for connection response data
 */
interface ConnectionResponse {
  success: boolean;
  message: string;
  data: {
    id: number;
    sourceNodeId: number;
    targetNodeId: number;
    label?: string;
    type?: string;
    weight?: number;
    threadId: number;
  };
}

/**
 * Connect two nodes together
 * @param sourceNodeId The ID of the source node
 * @param targetNodeId The ID of the target node
 * @param options Optional connection properties
 * @returns Promise with the response data
 */
export const connectNodes = async (
  sourceNodeId: number, 
  targetNodeId: number,
  options?: {
    label?: string;
    type?: string;
    weight?: number;
    color?: string;
  }
): Promise<ConnectionResponse> => {
  try {
    // First get the thread ID from the source node
    const sourceNodeResponse = await fetchWithAuth(
      API_ENDPOINTS.graph.nodes.get(sourceNodeId)
    );

    if (!sourceNodeResponse.ok) {
      const errorData = await sourceNodeResponse.json();
      throw new Error(errorData.message || `Error ${sourceNodeResponse.status}: ${sourceNodeResponse.statusText}`);
    }

    const sourceNodeData = await sourceNodeResponse.json();
    const threadId = sourceNodeData.data.threadId;

    // Then create the edge using the thread ID
    const response = await fetchWithAuth(
      API_ENDPOINTS.graph.edges.create(threadId),
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
          sourceNodeId: sourceNodeId.toString(),
          targetNodeId: targetNodeId.toString(),
          ...(options?.label && { label: options.label }),
          ...(options?.type && { type: options.type }),
          ...(options?.weight && { weight: options.weight.toString() }),
          ...(options?.color && { color: options.color })
        })
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error connecting nodes:', error);
    throw error;
  }
};

/**
 * Disconnect two nodes
 * @param sourceNodeId The ID of the source node
 * @param targetNodeId The ID of the target node
 * @returns Promise with the response data
 */
export const disconnectNodes = async (
  sourceNodeId: number, 
  targetNodeId: number
): Promise<{ success: boolean; message: string }> => {
  try {
    // First get the edge ID
    const sourceNodeResponse = await fetchWithAuth(
      API_ENDPOINTS.graph.nodes.get(sourceNodeId)
    );

    if (!sourceNodeResponse.ok) {
      const errorData = await sourceNodeResponse.json();
      throw new Error(errorData.message || `Error ${sourceNodeResponse.status}: ${sourceNodeResponse.statusText}`);
    }

    const sourceNodeData = await sourceNodeResponse.json();
    const threadId = sourceNodeData.data.threadId;

    // Get edges for the thread
    const edgesResponse = await fetchWithAuth(
      API_ENDPOINTS.graph.edges.getByThread(threadId)
    );

    if (!edgesResponse.ok) {
      const errorData = await edgesResponse.json();
      throw new Error(errorData.message || `Error ${edgesResponse.status}: ${edgesResponse.statusText}`);
    }

    const edgesData = await edgesResponse.json();
    const edge = edgesData.data.find(
      (e: any) => e.sourceNodeId === sourceNodeId && e.targetNodeId === targetNodeId
    );

    if (!edge) {
      throw new Error('Edge not found');
    }

    // Delete the edge using its ID
    const response = await fetchWithAuth(
      API_ENDPOINTS.graph.edges.delete(edge.id),
      {
        method: 'DELETE'
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error disconnecting nodes:', error);
    throw error;
  }
};

/**
 * Get all connections for a node
 * @param nodeId The ID of the node
 * @returns Promise with the connections data
 */
export const getNodeConnections = async (nodeId: number): Promise<any> => {
  try {
    // First get the thread ID
    const nodeResponse = await fetchWithAuth(
      API_ENDPOINTS.graph.nodes.get(nodeId)
    );

    if (!nodeResponse.ok) {
      const errorData = await nodeResponse.json();
      throw new Error(errorData.message || `Error ${nodeResponse.status}: ${nodeResponse.statusText}`);
    }

    const nodeData = await nodeResponse.json();
    const threadId = nodeData.data.threadId;

    // Get all edges for the thread
    const response = await fetchWithAuth(
      API_ENDPOINTS.graph.edges.getByThread(threadId)
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
    }

    const result = await response.json();
    
    // Filter to only include edges connected to this node
    result.data = result.data.filter((edge: any) => 
      edge.sourceNodeId === nodeId || edge.targetNodeId === nodeId
    );

    return result;
  } catch (error) {
    console.error('Error getting node connections:', error);
    throw error;
  }
}; 