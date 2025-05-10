import React, { useState, useEffect } from 'react';
import { FaEdit, FaTrash, FaExternalLinkAlt, FaPlus, FaLink, FaUnlink } from 'react-icons/fa';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';
import { getNodeConnections } from '../../utils/graphUtils';

interface GraphNode {
  id: number;
  label: string;
  threadId: number;
  xPosition: number;
  yPosition: number;
  color: string;
  shape: string;
  size: number;
  version: number;
  detailsId?: number;
}

interface NodeConnection {
  id: number;
  sourceNodeId: number;
  targetNodeId: number;
  label?: string;
  type?: string;
  weight?: number;
  threadId: number;
  connectedNode?: {
    id: number;
    label: string;
    color: string;
    shape: string;
  };
}

interface NodeDetailsDTO {
  id: number;
  label: string;
  description: string;
  wikidataEntityId: string;
  nodeId: number;
}

interface NodeDetailsProps {
  node: GraphNode;
  onClose: () => void;
  onEdit: (nodeId: number) => void;
  onDelete: (nodeId: number) => void;
  onConnectionChange?: () => void;
  allNodes?: GraphNode[]; // All nodes in the graph for connecting
}

const NodeDetails: React.FC<NodeDetailsProps> = ({ 
  node, 
  onClose, 
  onEdit, 
  onDelete,
  onConnectionChange,
  allNodes = []
}) => {
  const [nodeDetails, setNodeDetails] = useState<NodeDetailsDTO | null>(null);
  const [connections, setConnections] = useState<NodeConnection[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [loadingConnections, setLoadingConnections] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [connectionsError, setConnectionsError] = useState<string | null>(null);
  const [showConnectForm, setShowConnectForm] = useState<boolean>(false);
  const [selectedNodeToConnect, setSelectedNodeToConnect] = useState<number | ''>('');

  // Fetch node details when component mounts
  useEffect(() => {
    const fetchNodeDetails = async () => {
      // Only fetch if we have a detailsId
      if (!node.detailsId) {
        return;
      }

      setLoading(true);
      try {
        // First try to fetch from the nodeDetails endpoint
        let response = await fetchWithAuth(API_ENDPOINTS.graph.nodeDetails.get(node.detailsId));
        
        // If that fails (endpoint might not exist yet), try to fetch the node and extract details
        if (!response.ok) {
          console.warn('Node details endpoint not responding, fetching node instead');
          
          // Fallback: fetch the node and try to extract details
          response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.get(node.id));
          
          if (!response.ok) {
            throw new Error(`Failed to fetch node: ${response.status}`);
          }
          
          const nodeData = await response.json();
          
          // Check if the node has details embedded
          if (nodeData.data && nodeData.data.details) {
            setNodeDetails(nodeData.data.details);
          } else {
            // Create a placeholder with empty values
            setNodeDetails({
              id: node.detailsId || 0,
              label: node.label,
              description: "No detailed description available",
              wikidataEntityId: "",
              nodeId: node.id
            });
          }
        } else {
          // Original endpoint worked
          const data = await response.json();
          setNodeDetails(data.data);
        }
      } catch (err) {
        console.error('Error fetching node details:', err);
        setError('Failed to load node details');
        
        // Create a placeholder with empty values as fallback
        if (node.detailsId) {
          setNodeDetails({
            id: node.detailsId,
            label: node.label,
            description: "Details could not be loaded",
            wikidataEntityId: "",
            nodeId: node.id
          });
        }
      } finally {
        setLoading(false);
      }
    };

    fetchNodeDetails();
    fetchConnections();
  }, [node.detailsId, node.id, node.label]);

  // Fetch node connections
  const fetchConnections = async () => {
    setLoadingConnections(true);
    setConnectionsError(null);
    
    try {
      // Use the API endpoint that directly gets edges for a node
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.getByNode(node.id)
      );
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      
      // Fetch connected node details for each connection
      const connectionsWithNodes = await Promise.all(
        data.data.map(async (edge: any) => {
          const connectedNodeId = edge.sourceNodeId === node.id ? edge.targetNodeId : edge.sourceNodeId;
          let connectedNode = allNodes.find(n => n.id === connectedNodeId);
          
          if (!connectedNode) {
            try {
              const nodeResponse = await fetchWithAuth(
                API_ENDPOINTS.graph.nodes.get(connectedNodeId)
              );
              
              if (nodeResponse.ok) {
                const nodeData = await nodeResponse.json();
                connectedNode = nodeData.data;
              }
            } catch (err) {
              console.error(`Error fetching node ${connectedNodeId}:`, err);
            }
          }
          
          return {
            ...edge,
            connectedNode
          };
        })
      );
      
      setConnections(connectionsWithNodes);
    } catch (err) {
      console.error('Error fetching connections:', err);
      setConnectionsError('Failed to load connections');
    } finally {
      setLoadingConnections(false);
    }
  };

  // Connect to another node
  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!selectedNodeToConnect) {
      return;
    }
    
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.create(node.threadId),
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          body: new URLSearchParams({
            sourceNodeId: node.id.toString(),
            targetNodeId: selectedNodeToConnect.toString(),
            type: 'default',  // Default edge type
            weight: '1'       // Default weight
          })
        }
      );
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      // Refresh connections
      fetchConnections();
      setShowConnectForm(false);
      setSelectedNodeToConnect('');
      
      // Notify parent component about the connection change
      if (onConnectionChange) {
        onConnectionChange();
      }
    } catch (err) {
      console.error('Error connecting nodes:', err);
      setConnectionsError('Failed to connect nodes');
    }
  };

  // Disconnect from another node
  const handleDisconnect = async (connectionId: number) => {
    try {
      const connection = connections.find(c => c.id === connectionId);
      if (!connection) {
        throw new Error('Connection not found');
      }
      
      // Use the direct edge delete endpoint with the edge ID
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.delete(connectionId),
        {
          method: 'DELETE'
        }
      );
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      // Refresh connections
      fetchConnections();
      
      // Notify parent component about the connection change
      if (onConnectionChange) {
        onConnectionChange();
      }
    } catch (err) {
      console.error('Error disconnecting nodes:', err);
      setConnectionsError('Failed to disconnect nodes');
    }
  };

  // Handler for edit button
  const handleEdit = () => {
    onEdit(node.id);
    onClose(); // Close the details modal when opening edit modal
  };

  // Handler for delete button
  const handleDelete = () => {
    onDelete(node.id);
    onClose(); // Close the details modal after deletion
  };

  // Get list of nodes that can be connected (not already connected)
  const getConnectableNodes = () => {
    // Get list of already connected node IDs
    const connectedNodeIds = connections.map(conn => 
      conn.sourceNodeId === node.id ? conn.targetNodeId : conn.sourceNodeId
    );
    
    // Filter out nodes that are already connected and this node itself
    return allNodes.filter(n => 
      n.id !== node.id && !connectedNodeIds.includes(n.id)
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white/95 backdrop-blur-sm rounded-lg p-6 max-w-2xl w-full shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-semibold">Node Details</h3>
          <button 
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700"
          >
            ✕
          </button>
        </div>
        
        <div className="flex flex-row">
          {/* Left column: Node details */}
          <div className="flex-1">
            {/* Node visualization and basic info */}
            <div className="flex items-center mb-6">
              <div 
                className="w-14 h-14 mr-4 rounded-md shadow-md flex-shrink-0" 
                style={{ 
                  backgroundColor: node.color,
                  borderRadius: node.shape === 'circle' ? '50%' : '4px'
                }}
              ></div>
              <h4 className="text-xl font-medium">{node.label}</h4>
            </div>
            
            {/* Node properties */}
            <div className="space-y-4">
              <div>
                <p className="text-sm text-gray-500 mb-1">Node Properties</p>
                <div className="bg-gray-50 p-3 rounded-md">
                  <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                    <div>
                      <span className="font-medium text-gray-700">Shape:</span> 
                      <span className="ml-1 capitalize">{node.shape}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Size:</span> 
                      <span className="ml-1">{node.size}px</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Position:</span> 
                      <span className="ml-1">({node.xPosition.toFixed(0)}, {node.yPosition.toFixed(0)})</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Version:</span> 
                      <span className="ml-1">{node.version}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Node details from backend */}
              {loading ? (
                <div className="py-4 text-center text-gray-500">
                  Loading details...
                </div>
              ) : error ? (
                <div className="py-2 text-center text-red-500 text-sm">
                  {error}
                </div>
              ) : nodeDetails ? (
                <div className="space-y-3">
                  <div>
                    <p className="text-sm text-gray-500 mb-1">Wikidata Information</p>
                    <div className="bg-gray-50 p-3 rounded-md">
                      <h5 className="font-medium mb-1">{nodeDetails.label}</h5>
                      <p className="text-sm text-gray-700 mb-2">{nodeDetails.description}</p>
                      {nodeDetails.wikidataEntityId ? (
                        <a 
                          href={`https://www.wikidata.org/wiki/${nodeDetails.wikidataEntityId}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-xs text-blue-600 hover:underline flex items-center"
                        >
                          <FaExternalLinkAlt className="mr-1" size={10} />
                          View on Wikidata
                        </a>
                      ) : (
                        <p className="text-xs text-gray-500 italic">
                          No Wikidata entity linked
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ) : node.detailsId ? (
                <div className="py-2 text-center text-gray-500 text-sm">
                  No additional details found
                </div>
              ) : (
                <div className="bg-gray-50 p-3 rounded-md">
                  <p className="text-sm text-gray-500 mb-2">
                    This node doesn't have any linked information.
                  </p>
                  <p className="text-xs text-gray-500">
                    You can edit the node to add more details.
                  </p>
                </div>
              )}
              
              {/* Node connections */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <p className="text-sm text-gray-500">Connections</p>
                  
                  {/* Only show connect button if there are other nodes to connect to */}
                  {getConnectableNodes().length > 0 && (
                    <button 
                      onClick={() => setShowConnectForm(!showConnectForm)}
                      className="text-xs text-blue-600 hover:underline flex items-center"
                    >
                      <FaPlus className="mr-1" size={10} />
                      Add Connection
                    </button>
                  )}
                </div>
                
                {showConnectForm && (
                  <div className="bg-blue-50 p-3 mb-3 rounded-md">
                    <form onSubmit={handleConnect} className="flex items-center space-x-2">
                      <select
                        value={selectedNodeToConnect}
                        onChange={(e) => setSelectedNodeToConnect(e.target.value ? Number(e.target.value) : '')}
                        className="flex-1 text-sm p-1 border border-gray-300 rounded"
                        required
                      >
                        <option value="">Select a node to connect</option>
                        {getConnectableNodes().map(n => (
                          <option key={n.id} value={n.id}>{n.label}</option>
                        ))}
                      </select>
                      <button
                        type="submit"
                        className="bg-blue-500 text-white text-xs p-1 rounded flex items-center"
                        disabled={!selectedNodeToConnect}
                      >
                        <FaLink className="mr-1" size={10} />
                        Connect
                      </button>
                    </form>
                  </div>
                )}
                
                {loadingConnections ? (
                  <div className="py-3 text-center text-gray-500 text-sm">
                    Loading connections...
                  </div>
                ) : connectionsError ? (
                  <div className="py-2 text-center text-red-500 text-sm">
                    {connectionsError}
                  </div>
                ) : connections.length > 0 ? (
                  <div className="bg-gray-50 p-3 rounded-md">
                    <ul className="space-y-2">
                      {connections.map(conn => (
                        <li key={conn.id} className="flex justify-between items-center text-sm">
                          <div className="flex items-center">
                            {conn.connectedNode && (
                              <div 
                                className="w-4 h-4 mr-2 rounded-sm" 
                                style={{ 
                                  backgroundColor: conn.connectedNode.color,
                                  borderRadius: conn.connectedNode.shape === 'circle' ? '50%' : '2px'
                                }}
                              ></div>
                            )}
                            <span>{conn.connectedNode?.label || 
                              (conn.sourceNodeId === node.id 
                                ? `Node ${conn.targetNodeId}` 
                                : `Node ${conn.sourceNodeId}`)
                            }</span>
                            {conn.label && (
                              <span className="text-xs text-gray-500 ml-2">({conn.label})</span>
                            )}
                          </div>
                          <button
                            onClick={() => handleDisconnect(conn.id)}
                            className="text-red-500 hover:text-red-700"
                            title="Remove connection"
                          >
                            <FaUnlink size={12} />
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : (
                  <div className="bg-gray-50 p-3 rounded-md">
                    <p className="text-sm text-gray-500 text-center">
                      No connections found
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* Right column: Action buttons */}
          <div className="ml-4 flex flex-col justify-start space-y-3 pt-[70px]">
            <button
              onClick={handleEdit}
              className="p-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors flex items-center gap-2 shadow-sm w-16 justify-center"
              title="Edit Node"
            >
              <FaEdit size={16} />
            </button>
            <button
              onClick={handleDelete}
              className="p-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors flex items-center gap-2 shadow-sm w-16 justify-center"
              title="Delete Node"
            >
              <FaTrash size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NodeDetails; 