import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { FaThumbsUp, FaThumbsDown, FaRegThumbsUp, FaRegThumbsDown, FaPlus, FaLink, FaEdit, FaTrash, FaTimes } from 'react-icons/fa';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';
import Tag from '../tags/Tag';
import GraphVisualization from '../graph/GraphVisualization';
import { HexColorPicker } from 'react-colorful';
import NodeDetails from '../graph/NodeDetails';
import EdgeDetails from '../graph/EdgeDetails';
import CommentSection from '../comments/CommentSection';

interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface Thread {
  id: number;
  title: string;
  description: string | null;
  authorId: number;
  tags: Tag[];
  upvoteCount: number;
  downvoteCount: number;
  createdAt: string;
  updatedAt: string;
}

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

interface GraphEdge {
  id: number;
  sourceNodeId: number;
  targetNodeId: number;
  label: string;
  type: string;
  weight: number;
  color: string;
  threadId: number;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

interface VoteStatus {
  hasVoted: boolean;
  voteType: string | null;
  voteCount: number;
}

const ThreadDetail = () => {
  const { id } = useParams<{ id: string }>();
  const [thread, setThread] = useState<Thread | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [userVote, setUserVote] = useState<'UPVOTE' | 'DOWNVOTE' | null>(null);
  const [votingInProgress, setVotingInProgress] = useState(false);
  const [graphNodes, setGraphNodes] = useState<GraphNode[]>([]);
  const [graphEdges, setGraphEdges] = useState<GraphEdge[]>([]);
  const [graphLoading, setGraphLoading] = useState(true);
  
  // Node creation state
  const [showNodeForm, setShowNodeForm] = useState(false);
  const [nodeLabel, setNodeLabel] = useState('');
  const [nodeColor, setNodeColor] = useState('#4287f5');
  const [nodeShape, setNodeShape] = useState('circle');
  const [nodeSize, setNodeSize] = useState(50);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Node editing state
  const [showNodeEditModal, setShowNodeEditModal] = useState(false);
  const [editingNodeId, setEditingNodeId] = useState<number | null>(null);
  const [editNodeLabel, setEditNodeLabel] = useState('');
  const [editNodeColor, setEditNodeColor] = useState('');
  const [editNodeShape, setEditNodeShape] = useState('');
  const [editNodeSize, setEditNodeSize] = useState(0);
  const [isEditSubmitting, setIsEditSubmitting] = useState(false);
  
  // Node details state
  const [showNodeDetails, setShowNodeDetails] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState<number | null>(null);
  const [returnToDetailsAfterEdit, setReturnToDetailsAfterEdit] = useState(false);
  
  // Edge details state
  const [showEdgeDetails, setShowEdgeDetails] = useState(false);
  const [selectedEdgeId, setSelectedEdgeId] = useState<number | null>(null);
  
  const navigate = useNavigate();

  // Fetch thread vote status
  const fetchThreadVoteStatus = useCallback(async () => {
    if (!id) return;

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.votes.threadVoteStatus(Number(id)));
      
      if (response.ok) {
        const { data } = await response.json();
        if (data.hasVoted) {
          setUserVote(data.voteType as 'UPVOTE' | 'DOWNVOTE');
        } else {
          setUserVote(null);
        }
      }
    } catch (err) {
      console.error('Error fetching thread vote status:', err);
    }
  }, [id]);

  // Recalculate vote counts if they seem off
  const recalculateVoteCounts = useCallback(async () => {
    if (!id || !thread) return;
    
    // Only recalculate if there's a potential inconsistency
    // For example, if a user has voted but the corresponding count is 0
    if ((userVote === 'UPVOTE' && thread.upvoteCount === 0) || 
        (userVote === 'DOWNVOTE' && thread.downvoteCount === 0) ||
        (userVote && thread.upvoteCount > 0 && thread.downvoteCount > 0)) {
      
      console.log("Detected potential vote count inconsistency, recalculating...");
      
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.votes.recalculateThreadVotes(Number(id)), {
          method: 'POST'
        });
        
        if (response.ok) {
          // Refresh the thread data after recalculation
          const refreshResponse = await fetchWithAuth(API_ENDPOINTS.threads.get(Number(id)));
          if (refreshResponse.ok) {
            const { data } = await refreshResponse.json();
            setThread(data);
            console.log("Vote counts corrected:", data.upvoteCount, data.downvoteCount);
          }
        }
      } catch (err) {
        console.error('Error recalculating vote counts:', err);
      }
    }
  }, [id, thread, userVote]);

  useEffect(() => {
    const fetchThread = async () => {
      try {
        if (!id) {
          setError('Thread ID is required');
          setLoading(false);
          return;
        }

        const response = await fetchWithAuth(API_ENDPOINTS.threads.get(Number(id)), {
          headers: {
            'Accept': 'application/json'
          }
        });

        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          const errorData = await response.json();
          setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
          setLoading(false);
          return;
        }

        const { data } = await response.json();
        setThread(data);
        
        // Fetch vote status after thread is loaded
        await fetchThreadVoteStatus();
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchThread();
  }, [id, navigate, fetchThreadVoteStatus]);

  // Ensure vote counts are accurate after vote status is fetched
  useEffect(() => {
    if (thread && userVote) {
      recalculateVoteCounts();
    }
  }, [thread, userVote, recalculateVoteCounts]);

  useEffect(() => {
    const fetchGraphData = async () => {
      if (!id) return;
      
      try {
        setGraphLoading(true);
        
        // Fetch nodes
        const nodesResponse = await fetchWithAuth(API_ENDPOINTS.graph.nodes.getByThread(Number(id)));
        if (!nodesResponse.ok) {
          if (handleAuthError(nodesResponse, navigate)) return;
          console.error('Failed to load graph nodes');
          return;
        }
        const nodesData = await nodesResponse.json();
        setGraphNodes(nodesData.data || []);
        
        // Fetch edges
        const edgesResponse = await fetchWithAuth(API_ENDPOINTS.graph.edges.getByThread(Number(id)));
        if (!edgesResponse.ok) {
          if (handleAuthError(edgesResponse, navigate)) return;
          console.error('Failed to load graph edges');
          return;
        }
        const edgesData = await edgesResponse.json();
        
        // Ensure all edges have a color property
        const edgesWithColor = (edgesData.data || []).map((edge: {
          id: number;
          sourceNodeId: number;
          targetNodeId: number;
          label: string;
          type: string;
          weight: number;
          color?: string;
          threadId: number;
        }) => ({
          ...edge,
          color: edge.color || '#555555' // Default color if not present
        }));
        
        setGraphEdges(edgesWithColor);
      } catch (err) {
        console.error('Error fetching graph data:', err);
      } finally {
        setGraphLoading(false);
      }
    };
    
    fetchGraphData();
  }, [id, navigate]);

  const handleNodePositionChange = async (nodeId: number, x: number, y: number) => {
    try {
      console.log(`Updating node ${nodeId} position to x=${x}, y=${y}`);
      
      // Make sure we have valid coordinates
      if (x === null || y === null || isNaN(x) || isNaN(y)) {
        console.error('Invalid coordinates:', { x, y });
        return;
      }

      const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.update(nodeId), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          xPosition: x,
          yPosition: y
        })
      });

      if (!response.ok) {
        const errorData = await response.json();
        console.error('Error updating node position:', errorData);
        return;
      }

      // Update the node in the local state to avoid a full refresh
      setGraphNodes(prev => 
        prev.map(node => 
          node.id === nodeId 
            ? { ...node, xPosition: x, yPosition: y } 
            : node
        )
      );
    } catch (err) {
      console.error('Error updating node position:', err);
    }
  };

  const handleVote = async (isUpvote: boolean) => {
    if (!thread || votingInProgress) return;

    setVotingInProgress(true);
    setError(null);

    try {
      // If already voted the same way, remove the vote
      if ((isUpvote && userVote === 'UPVOTE') || (!isUpvote && userVote === 'DOWNVOTE')) {
        // Remove vote
        const response = await fetchWithAuth(API_ENDPOINTS.votes.removeThreadVote(thread.id), {
          method: 'DELETE'
        });
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          console.error('Failed to remove vote');
          return;
        }
        
        const { data } = await response.json();
        
        // Update thread with new vote counts
        setThread(prev => {
          if (!prev) return null;
          return {
            ...prev,
            upvoteCount: isUpvote ? Math.max(0, prev.upvoteCount - 1) : prev.upvoteCount,
            downvoteCount: !isUpvote ? Math.max(0, prev.downvoteCount - 1) : prev.downvoteCount
          };
        });
        
        setUserVote(null);
      } else {
        // Add or change vote - use the vote endpoint from the API
        try {
          // First try the votes.threadVote endpoint that handles change votes properly
          const url = `${API_ENDPOINTS.votes.threadVote(thread.id)}?isUpvote=${isUpvote}`;
          const response = await fetchWithAuth(url, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            }
          });
          
          if (response.ok) {
            const { data } = await response.json();
            
            // If changing vote type (from upvote to downvote or vice versa)
            if (userVote) {
              setThread(prev => {
                if (!prev) return null;
                return {
                  ...prev,
                  upvoteCount: isUpvote 
                    ? prev.upvoteCount + 1 
                    : Math.max(0, prev.upvoteCount - (userVote === 'UPVOTE' ? 1 : 0)),
                  downvoteCount: !isUpvote 
                    ? prev.downvoteCount + 1 
                    : Math.max(0, prev.downvoteCount - (userVote === 'DOWNVOTE' ? 1 : 0))
                };
              });
            } else {
              // If adding a new vote
              setThread(prev => {
                if (!prev) return null;
                return {
                  ...prev,
                  upvoteCount: isUpvote ? prev.upvoteCount + 1 : prev.upvoteCount,
                  downvoteCount: !isUpvote ? prev.downvoteCount + 1 : prev.downvoteCount
                };
              });
            }
            
            setUserVote(isUpvote ? 'UPVOTE' : 'DOWNVOTE');
          } else {
            // If that failed, try the legacy endpoint as fallback
            console.warn("Vote API failed, trying legacy endpoint");
            await handleLegacyVote(isUpvote);
          }
        } catch (err) {
          console.error("Error with new vote API, trying legacy endpoint:", err);
          await handleLegacyVote(isUpvote);
        }
      }
    } catch (err) {
      console.error('Error voting:', err);
      // Don't set error state here to avoid showing error to user
    } finally {
      setVotingInProgress(false);
    }
  };

  // Fallback to legacy voting endpoint if the new one fails
  const handleLegacyVote = async (isUpvote: boolean) => {
    if (!thread) return;
    
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.threads.vote(thread.id), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ isUpvote })
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error('Failed to vote using legacy endpoint');
        return;
      }
      
      // Refresh the thread data to ensure correct vote counts
      await fetchThreadVoteStatus();
      
      // If legacy vote succeeded, ensure the UI is updated
      setUserVote(isUpvote ? 'UPVOTE' : 'DOWNVOTE');
    } catch (err) {
      console.error('Error with legacy vote:', err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleCreateNode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !nodeLabel) return;
    
    setIsSubmitting(true);
    
    try {
      // Calculate a good position for the new node
      // If no nodes exist, place it in the center
      // If nodes exist, place it near but offset from existing nodes
      let xPosition = 250;
      let yPosition = 250;
      
      if (graphNodes.length > 0) {
        // Find the average position of existing nodes and offset slightly
        const avgX = graphNodes.reduce((sum, node) => sum + node.xPosition, 0) / graphNodes.length;
        const avgY = graphNodes.reduce((sum, node) => sum + node.yPosition, 0) / graphNodes.length;
        
        // Add a random offset (-100 to 100 pixels) to avoid direct overlap
        xPosition = avgX + (Math.random() * 200 - 100);
        yPosition = avgY + (Math.random() * 200 - 100);
        
        // Ensure positions are positive
        xPosition = Math.max(50, xPosition);
        yPosition = Math.max(50, yPosition);
      }
      
      const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.create(Number(id)), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
          label: nodeLabel,
          xPosition: xPosition.toString(),
          yPosition: yPosition.toString(),
          color: nodeColor,
          shape: nodeShape,
          size: nodeSize.toString()
        })
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error('Failed to create node');
        return;
      }
      
      const data = await response.json();
      
      // Add the new node to the state
      setGraphNodes([...graphNodes, data.data]);
      setNodeLabel('');
      setShowNodeForm(false);
    } catch (err) {
      console.error('Error creating node:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteNode = async (nodeId: number) => {
    if (!window.confirm('Are you sure you want to delete this node?')) {
      return;
    }
    
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.delete(nodeId), {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error('Failed to delete node');
        return;
      }
      
      // Remove the node from state
      setGraphNodes(prev => prev.filter(node => node.id !== nodeId));
      
      // Also remove any edges connected to this node
      setGraphEdges(prev => prev.filter(edge => 
        edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId
      ));
      
    } catch (err) {
      console.error('Error deleting node:', err);
    }
  };

  const handleEditNode = (nodeId: number) => {
    const node = graphNodes.find(n => n.id === nodeId);
    if (!node) return;
    
    // If we're editing from details view, we'll want to go back to it on cancel
    if (showNodeDetails) {
      setReturnToDetailsAfterEdit(true);
    } else {
      setReturnToDetailsAfterEdit(false);
    }
    
    // Set editing state
    setEditingNodeId(nodeId);
    setEditNodeLabel(node.label);
    setEditNodeColor(node.color);
    setEditNodeShape(node.shape);
    setEditNodeSize(node.size);
    setShowNodeEditModal(true);
  };

  const handleCancelNodeEdit = () => {
    setShowNodeEditModal(false);
    
    // If we came from node details, go back to it
    if (returnToDetailsAfterEdit && editingNodeId) {
      setSelectedNodeId(editingNodeId);
      setShowNodeDetails(true);
    }
    
    setEditingNodeId(null);
  };

  const handleSubmitNodeEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingNodeId) return;
    
    setIsEditSubmitting(true);
    
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.update(editingNodeId), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          label: editNodeLabel,
          color: editNodeColor,
          shape: editNodeShape,
          size: editNodeSize
        })
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error('Failed to update node');
        return;
      }
      
      const data = await response.json();
      
      // Update the node in the local state
      setGraphNodes(prev => 
        prev.map(node => 
          node.id === editingNodeId 
            ? { 
                ...node, 
                label: editNodeLabel,
                color: editNodeColor,
                shape: editNodeShape,
                size: editNodeSize
              } 
            : node
        )
      );
      
      // Close the modal
      setShowNodeEditModal(false);
      setEditingNodeId(null);
      // Reset the flag, we don't want to return to details after saving
      setReturnToDetailsAfterEdit(false);
    } catch (err) {
      console.error('Error updating node:', err);
    } finally {
      setIsEditSubmitting(false);
    }
  };

  const handleViewNodeDetails = (nodeId: number) => {
    setSelectedNodeId(nodeId);
    setShowNodeDetails(true);
  };

  const closeNodeDetails = () => {
    setShowNodeDetails(false);
    setSelectedNodeId(null);
  };

  const handleConnectionChange = useCallback(async () => {
    if (!id) return;
    
    try {
      // Fetch edges
      const edgesResponse = await fetchWithAuth(API_ENDPOINTS.graph.edges.getByThread(Number(id)));
      if (!edgesResponse.ok) {
        if (handleAuthError(edgesResponse, navigate)) return;
        console.error('Failed to load graph edges');
        return;
      }
      const edgesData = await edgesResponse.json();
      
      // Ensure all edges have a color property
      const edgesWithColor = (edgesData.data || []).map((edge: {
        id: number;
        sourceNodeId: number;
        targetNodeId: number;
        label: string;
        type: string;
        weight: number;
        color?: string;
        threadId: number;
      }) => ({
        ...edge,
        color: edge.color || '#555555' // Default color if not present
      }));
      
      setGraphEdges(edgesWithColor);
    } catch (err) {
      console.error('Error refreshing graph edges:', err);
    }
  }, [id, navigate]);

  // Handle edge edit
  const handleEditEdge = (edgeId: number) => {
    setSelectedEdgeId(edgeId);
    setShowEdgeDetails(true);
  };

  // Handle edge delete
  const handleDeleteEdge = async (edgeId: number) => {
    try {
      // Remove the edge from the local state first for immediate UI feedback
      setGraphEdges(prev => prev.filter(edge => edge.id !== edgeId));
      
      // The actual API call is handled by the EdgeDetails component
      console.log(`Edge ${edgeId} deleted successfully`);
    } catch (err) {
      console.error('Error handling edge deletion:', err);
    }
  };

  // Handle edge update
  const handleEdgeUpdate = useCallback(() => {
    // Refresh graph data
    const fetchGraphData = async () => {
      if (!id) return;
      
      try {
        setGraphLoading(true);
        
        // Fetch nodes
        const nodesResponse = await fetchWithAuth(API_ENDPOINTS.graph.nodes.getByThread(Number(id)));
        if (!nodesResponse.ok) {
          if (handleAuthError(nodesResponse, navigate)) return;
          console.error('Failed to load graph nodes');
          return;
        }
        const nodesData = await nodesResponse.json();
        setGraphNodes(nodesData.data || []);
        
        // Fetch edges
        const edgesResponse = await fetchWithAuth(API_ENDPOINTS.graph.edges.getByThread(Number(id)));
        if (!edgesResponse.ok) {
          if (handleAuthError(edgesResponse, navigate)) return;
          console.error('Failed to load graph edges');
          return;
        }
        const edgesData = await edgesResponse.json();
        
        // Ensure all edges have a color property
        const edgesWithColor = (edgesData.data || []).map((edge: {
          id: number;
          sourceNodeId: number;
          targetNodeId: number;
          label: string;
          type: string;
          weight: number;
          color?: string;
          threadId: number;
        }) => ({
          ...edge,
          color: edge.color || '#555555' // Default color if not present
        }));
        
        setGraphEdges(edgesWithColor);
      } catch (err) {
        console.error('Error fetching graph data:', err);
      } finally {
        setGraphLoading(false);
      }
    };
    
    fetchGraphData();
  }, [id, navigate]);

  const renderContent = () => {
    if (loading) {
      return (
        <div className="flex justify-center items-center h-[calc(100vh-80px)]">
          <p className="text-gray-600">Loading...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg">
          {error}
        </div>
      );
    }

    if (!thread) {
      return null;
    }

    return (
      <div className="flex flex-col gap-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h1 className="text-2xl font-semibold text-gray-900 mb-3">
              {thread.title}
            </h1>

            <div className="flex items-center gap-4 text-sm text-gray-500 mb-4">
              <span>Posted {formatDate(thread.createdAt)}</span>
              {thread.updatedAt !== thread.createdAt && (
                <span>(Edited {formatDate(thread.updatedAt)})</span>
              )}
            </div>

            {thread.description && (
              <div className="prose max-w-none mb-6">
                {thread.description}
              </div>
            )}

            {thread.tags.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-6">
                {thread.tags.map(tag => (
                  <Tag key={tag.id} tag={tag} />
                ))}
              </div>
            )}

            <div className="flex items-center gap-6">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleVote(true)}
                  className={`p-2 rounded-full hover:bg-gray-100 transition-colors ${
                    userVote === 'UPVOTE' ? 'text-blue-600' : 'text-gray-600'
                  }`}
                  disabled={votingInProgress}
                >
                  {userVote === 'UPVOTE' ? <FaThumbsUp size={18} /> : <FaRegThumbsUp size={18} />}
                </button>
                <span className="text-gray-600 font-medium">{thread.upvoteCount}</span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleVote(false)}
                  className={`p-2 rounded-full hover:bg-gray-100 transition-colors ${
                    userVote === 'DOWNVOTE' ? 'text-red-600' : 'text-gray-600'
                  }`}
                  disabled={votingInProgress}
                >
                  {userVote === 'DOWNVOTE' ? <FaThumbsDown size={18} /> : <FaRegThumbsDown size={18} />}
                </button>
                <span className="text-gray-600 font-medium">{thread.downvoteCount}</span>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-4">
            <div className="flex justify-between items-center mb-3">
              <h2 className="text-xl font-semibold text-gray-900">Thread Visualization</h2>
              <div className="flex gap-2">
                <button 
                  onClick={() => {
                    setShowNodeForm(true);
                  }}
                  className="bg-blue-500 text-white py-1 px-3 rounded-md flex items-center gap-1 hover:bg-blue-600 transition-colors"
                >
                  <FaPlus size={12} />
                  <span>Add Node</span>
                </button>
              </div>
            </div>
            
            {graphLoading ? (
              <div className="flex justify-center items-center h-[500px]">
                <p className="text-gray-600">Loading graph data...</p>
              </div>
            ) : graphNodes.length === 0 ? (
              <div className="flex flex-col justify-center items-center h-[500px]">
                <p className="text-gray-600 mb-4">No graph data available for this thread.</p>
                <p className="text-gray-500 mb-4">Use the 'Add Node' button above to create your first node.</p>
              </div>
            ) : (
              <GraphVisualization 
                nodes={graphNodes} 
                edges={graphEdges} 
                onNodePositionChange={handleNodePositionChange}
                onNodeDelete={handleDeleteNode}
                onNodeEdit={handleEditNode}
                onNodeDetails={handleViewNodeDetails}
                onConnectionChange={handleConnectionChange}
                onEdgeEdit={handleEditEdge}
              />
            )}
          </div>
        </div>
        
        {/* Comment Section */}
        {thread && <CommentSection threadId={thread.id} />}
      </div>
    );
  };

  return (
    <MainLayout>
      {() => (
        <>
          {renderContent()}
          
          {/* Node Create Modal */}
          {showNodeForm && (
            <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
              <div className="bg-white/95 backdrop-blur-sm rounded-lg p-6 max-w-md w-full shadow-xl">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="text-xl font-semibold">Create New Node</h3>
                  <button 
                    onClick={() => setShowNodeForm(false)}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    <FaTimes />
                  </button>
                </div>
                
                <form onSubmit={handleCreateNode} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Label</label>
                    <input
                      type="text"
                      value={nodeLabel}
                      onChange={(e) => setNodeLabel(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Enter node label"
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Color</label>
                    <div className="flex flex-col md:flex-row gap-4">
                      <div className="w-full md:w-1/2">
                        <HexColorPicker 
                          color={nodeColor} 
                          onChange={setNodeColor} 
                          style={{ width: '100%', height: '150px' }}
                        />
                      </div>
                      <div className="flex-1 flex flex-col">
                        <input
                          type="text"
                          value={nodeColor}
                          onChange={(e) => setNodeColor(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md mb-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <div className="h-12 w-full rounded-md mt-2 border border-gray-200" style={{ backgroundColor: nodeColor }}></div>
                      </div>
                    </div>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Shape</label>
                    <select
                      value={nodeShape}
                      onChange={(e) => setNodeShape(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="circle">Circle</option>
                      <option value="rectangle">Rectangle</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Node Size: {nodeSize}
                    </label>
                    <input
                      type="range"
                      min="20"
                      max="100"
                      value={nodeSize}
                      onChange={(e) => setNodeSize(parseInt(e.target.value))}
                      className="w-full accent-blue-500"
                    />
                  </div>
                  
                  <div className="flex justify-end gap-3">
                    <button
                      type="button"
                      onClick={() => setShowNodeForm(false)}
                      className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
                      disabled={isSubmitting}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
                      disabled={isSubmitting || !nodeLabel}
                    >
                      {isSubmitting ? 'Creating...' : 'Create Node'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
          
          {/* Node Edit Modal */}
          {showNodeEditModal && (
            <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
              <div className="bg-white/95 backdrop-blur-sm rounded-lg p-6 max-w-md w-full shadow-xl">
                <h3 className="text-xl font-semibold mb-4">Edit Node</h3>
                <form onSubmit={handleSubmitNodeEdit}>
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Label</label>
                    <input
                      type="text"
                      value={editNodeLabel}
                      onChange={(e) => setEditNodeLabel(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      required
                    />
                  </div>
                  
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Color</label>
                    <div className="flex flex-col md:flex-row gap-4">
                      <div className="w-full md:w-1/2">
                        <HexColorPicker 
                          color={editNodeColor} 
                          onChange={setEditNodeColor} 
                          style={{ width: '100%', height: '150px' }}
                        />
                      </div>
                      <div className="flex-1 flex flex-col">
                        <input
                          type="text"
                          value={editNodeColor}
                          onChange={(e) => setEditNodeColor(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md mb-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <div className="h-12 w-full rounded-md mt-2 border border-gray-200 shadow-sm" style={{ backgroundColor: editNodeColor }}></div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-2">Node Shape</label>
                    <select
                      value={editNodeShape}
                      onChange={(e) => setEditNodeShape(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="circle">Circle</option>
                      <option value="rectangle">Rectangle</option>
                    </select>
                  </div>
                  
                  <div className="mb-5">
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Node Size: {editNodeSize}
                    </label>
                    <input
                      type="range"
                      min="20"
                      max="100"
                      value={editNodeSize}
                      onChange={(e) => setEditNodeSize(parseInt(e.target.value))}
                      className="w-full accent-blue-500"
                    />
                  </div>
                  
                  <div className="flex justify-end gap-3">
                    <button
                      type="button"
                      onClick={handleCancelNodeEdit}
                      className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors shadow-sm"
                      disabled={isEditSubmitting}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors shadow-sm"
                      disabled={isEditSubmitting || !editNodeLabel}
                    >
                      {isEditSubmitting ? 'Saving...' : 'Save Changes'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
          
          {/* Node Details Modal */}
          {showNodeDetails && selectedNodeId && (
            <NodeDetails
              node={graphNodes.find(node => node.id === selectedNodeId)!}
              onClose={closeNodeDetails}
              onEdit={handleEditNode}
              onDelete={handleDeleteNode}
              onConnectionChange={handleConnectionChange}
              allNodes={graphNodes}
            />
          )}
          
          {/* Edge Details Modal */}
          {showEdgeDetails && selectedEdgeId && (
            <EdgeDetails
              edge={graphEdges.find(edge => edge.id === selectedEdgeId)!}
              onClose={() => {
                setShowEdgeDetails(false);
                setSelectedEdgeId(null);
              }}
              onUpdate={handleEdgeUpdate}
              onDelete={handleDeleteEdge}
            />
          )}
        </>
      )}
    </MainLayout>
  );
};

export default ThreadDetail; 