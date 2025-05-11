import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { FaThumbsUp, FaThumbsDown, FaRegThumbsUp, FaRegThumbsDown, FaPlus, FaLink, FaEdit, FaTrash, FaTimes, FaUserMinus, FaUserPlus, FaEllipsisV, FaSearch, FaHistory, FaExclamationTriangle, FaArrowsAlt, FaCompass } from 'react-icons/fa';
import { BiNetworkChart } from 'react-icons/bi';
import { fetchWithAuth, handleAuthError, canEditThread } from '../../utils/authUtils';
import { addToRecentThreads } from '../../utils/recentThreadsUtils';
import Tag from '../tags/Tag';
import GraphVisualization from '../graph/GraphVisualization';
import { HexColorPicker } from 'react-colorful';
import NodeDetails from '../graph/NodeDetails';
import EdgeDetails from '../graph/EdgeDetails';
import CommentSection from '../comments/CommentSection';
import eventBus, { EVENTS } from '../../utils/eventBus';
import { isProfanityError, formatProfanityError, ProfanityErrorMessage } from '../../utils/errorUtils';

interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface Author {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
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
  active: boolean;
  deactivatedByRole: string | null;
  followerIds?: number[];
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
  const [formError, setFormError] = useState<string | null>(null); // Error for forms only
  const [userVote, setUserVote] = useState<'UPVOTE' | 'DOWNVOTE' | null>(null);
  const [votingInProgress, setVotingInProgress] = useState(false);
  const [graphNodes, setGraphNodes] = useState<GraphNode[]>([]);
  const [graphEdges, setGraphEdges] = useState<GraphEdge[]>([]);
  const [graphLoading, setGraphLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState<{id: number} | null>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);
  
  // Graph interaction mode state
  const [interactionMode, setInteractionMode] = useState<'move' | 'connect'>('move');
  
  // Graph search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<GraphNode[]>([]);
  const [highlightedNodeId, setHighlightedNodeId] = useState<number | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  
  // Reference to the graph visualization instance
  const graphRef = useRef<any>(null);
  
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
  
  // Author state
  const [author, setAuthor] = useState<Author | null>(null);
  const [authorLoading, setAuthorLoading] = useState(false);
  
  // Thread options menu state
  const [showOptionsMenu, setShowOptionsMenu] = useState(false);
  const optionsMenuRef = useRef<HTMLDivElement>(null);
  const [canEdit, setCanEdit] = useState(false);
  
  const navigate = useNavigate();

  // CSS for animations
  useEffect(() => {
    // Add keyframes for the fade-in animation
    const style = document.createElement('style');
    style.textContent = `
      @keyframes fadeIn {
        from { opacity: 0; transform: translateY(-10px); }
        to { opacity: 1; transform: translateY(0); }
      }
      .animate-fade-in {
        animation: fadeIn 0.3s ease-out forwards;
      }
      
      /* Tooltip styling */
      .tooltip {
        position: relative;
      }
      
      .tooltip:hover::after {
        content: attr(title);
        position: absolute;
        bottom: -30px;
        left: 50%;
        transform: translateX(-50%);
        background-color: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 12px;
        white-space: nowrap;
        z-index: 100;
        pointer-events: none;
      }
      
      .tooltip:hover::before {
        content: '';
        position: absolute;
        bottom: -10px;
        left: 50%;
        transform: translateX(-50%);
        border-width: 5px;
        border-style: solid;
        border-color: transparent transparent rgba(0, 0, 0, 0.8) transparent;
        z-index: 100;
        pointer-events: none;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  // Close options menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (optionsMenuRef.current && !optionsMenuRef.current.contains(event.target as Node)) {
        setShowOptionsMenu(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

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
        (userVote === 'DOWNVOTE' && thread.downvoteCount === 0)) {
      
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

  // Fetch current user
  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.users.me);
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          return;
        }
        
        const { data } = await response.json();
        setCurrentUser(data);
      } catch (err) {
        console.error('Error fetching current user:', err);
      }
    };
    
    fetchCurrentUser();
  }, [navigate]);

  // Check if current user is following the thread
  useEffect(() => {
    if (currentUser && thread && thread.followerIds) {
      setIsFollowing(thread.followerIds.includes(currentUser.id));
    }
  }, [currentUser, thread]);

  // Add function to fetch author details
  const fetchAuthor = useCallback(async (authorId: number) => {
    setAuthorLoading(true);
    try {
      const response = await fetchWithAuth(`${API_ENDPOINTS.users.all}/${authorId}`);
      
      if (response.ok) {
        const result = await response.json();
        if (result && result.data) {
          setAuthor({
            id: result.data.id,
            firstName: result.data.firstName,
            lastName: result.data.lastName,
            username: result.data.username
          });
        }
      } else {
        console.error('Failed to fetch author');
      }
    } catch (err) {
      console.error('Error fetching author:', err);
    } finally {
      setAuthorLoading(false);
    }
  }, []);

  // Update the useEffect where thread is fetched to also fetch author
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
        
        // Fetch the author if we have author ID
        if (data.authorId) {
          fetchAuthor(data.authorId);
        }
        
        // Add thread to recently viewed list in local storage
        addToRecentThreads(data.id, data.title);
        
        // Emit event to notify other components (like Sidebar) that a thread was viewed
        eventBus.emit(EVENTS.THREAD_VIEWED, {
          id: data.id,
          title: data.title
        });
        
        // Fetch vote status after thread is loaded
        await fetchThreadVoteStatus();
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchThread();
  }, [id, navigate, fetchThreadVoteStatus, fetchAuthor]);

  // Ensure vote counts are accurate after vote status is fetched
  useEffect(() => {
    if (thread && userVote) {
      recalculateVoteCounts();
    }
  }, [thread, userVote, recalculateVoteCounts]);

  // Add fetchGraphData declaration reference at the top, before it's used
  const fetchGraphData = useCallback(async () => {
    if (!id) return;
    
    setGraphLoading(true);
    
    try {
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
      const edgesWithColor = (edgesData.data || []).map((edge: GraphEdge) => ({
        ...edge,
        color: edge.color || '#555555' // Default color if not present
      }));
      
      setGraphEdges(edgesWithColor);
    } catch (err) {
      console.error('Error fetching graph data:', err);
    } finally {
      setGraphLoading(false);
    }
  }, [id, navigate, handleAuthError]);

  // Add useEffect to fetch graph data when the component mounts or thread ID changes
  useEffect(() => {
    if (id) {
      fetchGraphData();
    }
  }, [id, navigate, fetchGraphData]);

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
    if (!id) return;
    
    setIsSubmitting(true);
    setFormError(null);
    
    try {
      const nodeData = {
        label: nodeLabel,
        threadId: Number(id),
        xPosition: 100,
        yPosition: 100,
        color: nodeColor,
        shape: nodeShape,
        size: nodeSize
      };
      
      // Try to create the node without changing page state on error
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.create(Number(id)), {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(nodeData)
        });
        
        if (!response.ok) {
          // Handle authentication errors but skip redirects
          if (response.status === 401) {
            setFormError("Authentication error. Please log in again.");
            return;
          }
          
          const errorResponse = await response.json();
          
          // Look for profanity-related messages in the error
          if (errorResponse && errorResponse.message && 
             (errorResponse.message.toLowerCase().includes('inappropriate language') ||
              errorResponse.message.toLowerCase().includes('profanity'))) {
            setFormError("Your node label contains inappropriate language. Please revise it.");
            return;
          }
          
          setFormError(errorResponse.message || 'Failed to create node');
          return;
        }
        
        // Success - reset form and refresh graph data
        setNodeLabel('');
        setNodeColor('#4287f5');
        setNodeShape('circle');
        setNodeSize(50);
        setShowNodeForm(false);
        await fetchGraphData();
      } catch (err) {
        console.error('Error creating node:', err);
        setFormError('Failed to create node. Please try again.');
      }
    } catch (err) {
      console.error('Error in node creation process:', err);
      setFormError('An unexpected error occurred. Please try again.');
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
    setFormError(null);
    
    try {
      const nodeData = {
        id: editingNodeId,
        label: editNodeLabel,
        color: editNodeColor,
        shape: editNodeShape,
        size: editNodeSize
      };
      
      // Try to update the node without changing page state on error
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.graph.nodes.update(editingNodeId), {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(nodeData)
        });
        
        if (!response.ok) {
          // Handle authentication errors but skip redirects
          if (response.status === 401) {
            setFormError("Authentication error. Please log in again.");
            return;
          }
          
          const errorResponse = await response.json();
          
          // Look for profanity-related messages in the error
          if (errorResponse && errorResponse.message && 
             (errorResponse.message.toLowerCase().includes('inappropriate language') ||
              errorResponse.message.toLowerCase().includes('profanity'))) {
            setFormError("Your node label contains inappropriate language. Please revise it.");
            return;
          }
          
          setFormError(errorResponse.message || 'Failed to update node');
          return;
        }
        
        // Success - close modal and refresh graph
        setShowNodeEditModal(false);
        
        // Show details again if we were looking at them before
        if (returnToDetailsAfterEdit && selectedNodeId) {
          setShowNodeDetails(true);
        }
        
        await fetchGraphData();
      } catch (err) {
        console.error('Error updating node:', err);
        setFormError('Failed to update node. Please try again.');
      }
    } catch (err) {
      console.error('Error in node edit process:', err);
      setFormError('An unexpected error occurred. Please try again.');
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
    // Call the fetchGraphData function to refresh graph data
    fetchGraphData();
  }, [id, navigate, fetchGraphData]);

  const handleFollowToggle = async () => {
    if (!thread || !currentUser) return;
    
    setFollowLoading(true);
    try {
      const isUnfollowing = isFollowing;
      const endpoint = isUnfollowing 
        ? API_ENDPOINTS.threads.unfollow(thread.id)
        : API_ENDPOINTS.threads.follow(thread.id);
      
      const response = await fetchWithAuth(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        throw new Error('Failed to follow/unfollow thread');
      }
      
      // Update follow status
      setIsFollowing(!isFollowing);
      
      // Refresh thread data to update followers list
      const threadResponse = await fetchWithAuth(API_ENDPOINTS.threads.get(Number(id)));
      if (threadResponse.ok) {
        const { data } = await threadResponse.json();
        setThread(data);
        
        // Emit event to notify other components about the follow/unfollow action
        if (isUnfollowing) {
          eventBus.emit(EVENTS.THREAD_UNFOLLOWED, data.id);
        } else {
          eventBus.emit(EVENTS.THREAD_FOLLOWED, data.id);
        }
      }
    } catch (err) {
      console.error('Error while following/unfollowing:', err);
    } finally {
      setFollowLoading(false);
    }
  };

  const handleEditThread = () => {
    setShowOptionsMenu(false);
    navigate(`/threads/${id}/edit`);
  };
  
  const handleResearchWikidata = () => {
    // Close the menu
    setShowOptionsMenu(false);
    
    // Implement Wikidata research functionality
    alert("Wikidata research functionality will be implemented here");
    // In the future, this could make API calls to fetch Wikidata information
    // related to the thread's tags or title
  };

  const handleViewThreadHistory = () => {
    // Close the menu
    setShowOptionsMenu(false);
    
    // Navigate to thread history page
    if (thread) {
      navigate(`/threads/${thread.id}/history`);
    }
  };

  // Check if user can edit the thread
  useEffect(() => {
    const checkEditPermission = async () => {
      if (!id || !currentUser) return;
      
      try {
        const canEditResult = await canEditThread(Number(id));
        setCanEdit(canEditResult);
      } catch (error) {
        console.error('Error checking edit permission:', error);
      }
    };
    
    if (currentUser && id) {
      checkEditPermission();
    }
  }, [currentUser, id]);

  // Function to search nodes by label
  const searchNodes = useCallback((query: string) => {
    if (!query.trim()) {
      setSearchResults([]);
      setHighlightedNodeId(null);
      return;
    }
    
    const normalizedQuery = query.toLowerCase().trim();
    const matchingNodes = graphNodes.filter(node => 
      node.label.toLowerCase().includes(normalizedQuery)
    );
    
    setSearchResults(matchingNodes);
    
    // If we have results, highlight the first one
    if (matchingNodes.length > 0) {
      const firstMatch = matchingNodes[0];
      setHighlightedNodeId(firstMatch.id);
      
      // Zoom to the node
      if (graphRef.current && graphRef.current.zoomToNode) {
        graphRef.current.zoomToNode(firstMatch.id);
      }
    } else {
      setHighlightedNodeId(null);
    }
  }, [graphNodes]);
  
  // Handle search input changes
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const query = e.target.value;
    setSearchQuery(query);
    searchNodes(query);
  };
  
  // Toggle search input visibility
  const toggleSearch = () => {
    setIsSearching(!isSearching);
    if (!isSearching) {
      // When opening the search, clear previous results
      setSearchQuery('');
      setSearchResults([]);
      setHighlightedNodeId(null);
    }
  };

  // Function to center the graph view
  const centerGraphView = () => {
    if (graphRef.current && graphRef.current.centerView) {
      graphRef.current.centerView();
    }
  };

  // Add keyboard shortcut for search
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl+F or Cmd+F to open search
      if ((e.ctrlKey || e.metaKey) && e.key === 'f' && graphNodes.length > 0) {
        e.preventDefault(); // Prevent browser's default search
        toggleSearch();
      }
      
      // Escape to close search
      if (e.key === 'Escape' && isSearching) {
        toggleSearch();
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isSearching, toggleSearch, graphNodes.length]);

  const renderContent = () => {
    if (loading) {
      return (
        <div className="flex justify-center items-center h-full">
          <p className="text-gray-500">Loading thread...</p>
        </div>
      );
    }
    
    if (error) {
      return (
        <div className="container mx-auto px-4 py-8">
          <div className="p-4 bg-red-50 border border-red-200 rounded-md">
            <div className="flex items-center">
              <div className="text-red-600 mr-3">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <p className="text-sm font-medium text-red-800">{error}</p>
            </div>
          </div>
        </div>
      );
    }
    
    if (!thread) {
      return (
        <div className="container mx-auto px-4 py-8">
          <p className="text-red-600">Thread not found.</p>
        </div>
      );
    }

    return (
      <div className="flex flex-col gap-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm p-6 order-1">
            <div className="flex items-center justify-between mb-2">
              <div className="ml-auto"></div>
            </div>
            <div className="border-b border-gray-100 pb-3 mb-3">
              <div className="flex flex-col md:flex-row md:justify-between md:items-start gap-2 mb-2">
                <h1 className="text-xl md:text-2xl font-semibold text-gray-800 leading-tight">
                  {thread.title}
                </h1>
                
                <div className="flex flex-row items-center gap-2 justify-end">
                  {currentUser && (
                    <button
                      onClick={handleFollowToggle}
                      disabled={followLoading}
                      className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-medium transition-all duration-200 shadow-sm ${
                        isFollowing
                          ? 'bg-gray-100 hover:bg-gray-200 text-gray-700 hover:shadow'
                          : 'bg-blue-500 hover:bg-blue-600 text-white hover:shadow-md'
                      } whitespace-nowrap`}
                    >
                      {followLoading ? (
                        <span className="inline-flex items-center">
                          <svg className="animate-spin -ml-1 mr-2 h-3 w-3 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                          Loading
                        </span>
                      ) : isFollowing ? (
                        <>
                          <FaUserMinus size={12} />
                          <span>Unfollow</span>
                        </>
                      ) : (
                        <>
                          <FaUserPlus size={12} />
                          <span>Follow</span>
                        </>
                      )}
                    </button>
                  )}
                  
                  {/* Thread Options Menu */}
                  <div className="relative" ref={optionsMenuRef}>
                    <button
                      onClick={() => setShowOptionsMenu(!showOptionsMenu)}
                      className="text-gray-500 hover:text-gray-700 p-1.5 rounded-full hover:bg-gray-100"
                      title="Thread options"
                    >
                      <FaEllipsisV size={14} />
                    </button>
                    
                    {showOptionsMenu && (
                      <div className="absolute right-0 mt-1 w-48 bg-white rounded-md shadow-lg z-10 py-1 border border-gray-200">
                        {canEdit && (
                          <button 
                            onClick={handleEditThread}
                            className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                          >
                            <FaEdit size={14} className="text-gray-500" />
                            <span>Edit Thread</span>
                          </button>
                        )}
                        <button 
                          onClick={handleResearchWikidata}
                          className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                        >
                          <FaSearch size={14} className="text-gray-500" />
                          <span>Research on Wikidata</span>
                        </button>
                        <button 
                          onClick={handleViewThreadHistory}
                          className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-2"
                        >
                          <FaHistory size={14} className="text-gray-500" />
                          <span>View Thread History</span>
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Thread Tags - Above author information */}
              {thread.tags.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mb-2">
                  {thread.tags.map(tag => (
                    <Tag key={tag.id} tag={tag} />
                  ))}
                </div>
              )}
            </div>
            
            {/* Thread inactive notice */}
            {thread && !thread.active && (
              <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-md flex items-center">
                <FaExclamationTriangle className="text-yellow-500 mr-2" />
                <span className="text-sm text-yellow-700">
                  This thread has been deactivated and is not visible to other users.
                  {thread.deactivatedByRole === 'ADMIN' && 
                    ' It was deactivated by an admin.'}
                </span>
              </div>
            )}
            
            {/* New separator */}
            <div className="border-t-2 border-gray-200 my-4"></div>
            
            {/* Author and Metadata - Improved layout */}
            <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-4">
              {/* Author Card */}
              <div className="flex items-center bg-gray-50 rounded-lg p-2 shadow-sm flex-shrink-0">
                {authorLoading ? (
                  <div className="flex items-center text-gray-500 text-sm">
                    <span className="animate-pulse">Loading author...</span>
                  </div>
                ) : author ? (
                  <Link 
                    to={`/users/${author.id}`}
                    className="flex items-center text-gray-600 hover:text-blue-600 transition-colors"
                  >
                    <div className={`w-10 h-10 rounded-full bg-blue-500 text-white flex items-center justify-center font-bold text-sm mr-2`}>
                      {author.username?.[0]?.toUpperCase()}
                    </div>
                    <div>
                      <div className="font-medium text-gray-900">
                        @{author.username}
                        <span className="ml-1 sm:ml-2 text-xs sm:text-sm bg-yellow-100 text-yellow-700 px-1 sm:px-2 py-0.5 rounded-full inline-block mt-1 sm:mt-0 sm:inline">
                          Thread Author
                        </span>
                      </div>
                    </div>
                  </Link>
                ) : (
                  <div className="text-sm text-gray-500">Author unavailable</div>
                )}
              </div>
              
              <div className="flex flex-wrap items-center gap-2 mt-2 sm:mt-0">
                <div className="flex items-center text-xs text-gray-500 bg-gray-50 px-2 py-1 rounded-full">
                  <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                  <span className="whitespace-nowrap">Posted {formatDate(thread.createdAt)}</span>
                </div>
                
                {thread.updatedAt !== thread.createdAt && (
                  <div className="flex items-center text-xs text-gray-500 bg-gray-50 px-2 py-1 rounded-full">
                    <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                    </svg>
                    <span>Edited {formatDate(thread.updatedAt)}</span>
                  </div>
                )}
                
                {thread.followerIds && (
                  <div className="flex items-center text-xs text-gray-500 bg-gray-50 px-2 py-1 rounded-full">
                    <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path>
                    </svg>
                    <span>{thread.followerIds.length} followers</span>
                  </div>
                )}
              </div>
            </div>
            
            {/* New separator */}
            <div className="border-t-2 border-gray-200 my-4"></div>

            {/* Thread Content */}
            {thread.description && (
              <>
                <h3 className="text-md font-medium text-gray-700 mb-2">Thread Description</h3>
                <div className="prose prose-base sm:prose-lg max-w-none my-6 px-4 py-3 bg-gray-50 rounded-lg border border-gray-100 shadow-inner overflow-hidden break-words">
                  {thread.description}
                </div>
              </>
            )}
            
            {/* New separator */}
            <div className="border-t-2 border-gray-200 my-4"></div>

            {/* Voting - Improved */}
            <div className="flex flex-wrap items-center gap-4 pt-3">
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => handleVote(true)}
                  className={`p-1.5 rounded-full hover:bg-blue-50 transition-colors ${
                    userVote === 'UPVOTE' ? 'text-blue-600 bg-blue-50' : 'text-gray-500'
                  }`}
                  disabled={votingInProgress}
                  title="Upvote"
                >
                  {userVote === 'UPVOTE' ? <FaThumbsUp size={16} /> : <FaRegThumbsUp size={16} />}
                </button>
                <span className={`font-medium text-sm ${userVote === 'UPVOTE' ? 'text-blue-600' : 'text-gray-500'}`}>
                  {thread.upvoteCount}
                </span>
              </div>

              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => handleVote(false)}
                  className={`p-1.5 rounded-full hover:bg-red-50 transition-colors ${
                    userVote === 'DOWNVOTE' ? 'text-red-600 bg-red-50' : 'text-gray-500'
                  }`}
                  disabled={votingInProgress}
                  title="Downvote"
                >
                  {userVote === 'DOWNVOTE' ? <FaThumbsDown size={16} /> : <FaRegThumbsDown size={16} />}
                </button>
                <span className={`font-medium text-sm ${userVote === 'DOWNVOTE' ? 'text-red-600' : 'text-gray-500'}`}>
                  {thread.downvoteCount}
                </span>
              </div>
              
              <button 
                className="ml-auto text-gray-400 hover:text-blue-500 flex items-center gap-1 text-xs"
                onClick={() => {
                  const url = window.location.href;
                  navigator.clipboard.writeText(url).then(() => {
                    alert("Link copied to clipboard!");
                  });
                }}
                title="Copy link to thread"
              >
                <FaLink size={12} />
                <span>Share</span>
              </button>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-4 order-2">
            <div className="flex justify-end items-center mb-3">
              <div className="flex gap-2 items-center">
                {/* Search input */}
                {isSearching && (
                  <div className="relative">
                    <input 
                      type="text"
                      className="py-1 pl-8 pr-2 text-sm border border-gray-300 rounded-full w-48 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:border-transparent"
                      placeholder="Search nodes..."
                      value={searchQuery}
                      onChange={handleSearchInputChange}
                      autoFocus
                    />
                    <FaSearch className="absolute left-3 top-2 text-gray-400" size={12} />
                    {searchQuery && (
                      <button 
                        className="absolute right-3 top-2 text-gray-400 hover:text-gray-600"
                        onClick={() => {
                          setSearchQuery('');
                          setSearchResults([]);
                          setHighlightedNodeId(null);
                        }}
                      >
                        <FaTimes size={12} />
                      </button>
                    )}
                    {searchResults.length > 0 && (
                      <div className="absolute top-9 left-0 w-full bg-white shadow-md rounded-md border border-gray-100 z-10">
                        <div className="p-2 text-xs text-gray-500 border-b border-gray-100">
                          Found {searchResults.length} results
                        </div>
                        <div className="max-h-40 overflow-y-auto">
                          {searchResults.map(node => (
                            <button
                              key={node.id}
                              className={`block w-full text-left px-3 py-2 text-sm hover:bg-blue-50 ${
                                highlightedNodeId === node.id ? 'bg-blue-100' : ''
                              }`}
                              onClick={() => {
                                setHighlightedNodeId(node.id);
                                if (graphRef.current && graphRef.current.zoomToNode) {
                                  graphRef.current.zoomToNode(node.id);
                                }
                              }}
                            >
                              {node.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                    {searchQuery && searchResults.length === 0 && (
                      <div className="absolute top-9 left-0 w-full bg-white shadow-md rounded-md border border-gray-100 z-10 p-3 text-center text-gray-500 text-sm">
                        No nodes found matching "{searchQuery}"
                      </div>
                    )}
                  </div>
                )}
                
                {/* Search button */}
                <button
                  onClick={toggleSearch}
                  className={`tooltip p-2 rounded-full flex items-center justify-center shadow hover:shadow-md transition-all duration-150 transform hover:scale-105 active:scale-95 ${
                    isSearching ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                  title={isSearching ? 'Close Search' : 'Search Nodes'}
                >
                  <FaSearch size={16} />
                </button>
                
                {/* Mode toggle button */}
                <button
                  onClick={() => setInteractionMode(interactionMode === 'move' ? 'connect' : 'move')}
                  className={`tooltip p-2 rounded-full flex items-center justify-center shadow hover:shadow-md transition-all duration-150 transform hover:scale-105 active:scale-95 ${
                    interactionMode === 'move' 
                      ? 'bg-blue-100 text-blue-700 hover:bg-blue-200' 
                      : 'bg-green-100 text-green-700 hover:bg-green-200'
                  }`}
                  title={interactionMode === 'move' ? 'Switch to Connect Mode' : 'Switch to Move Mode'}
                >
                  {interactionMode === 'move' ? (
                    <BiNetworkChart size={18} />
                  ) : (
                    <FaArrowsAlt size={16} />
                  )}
                </button>

                {/* Center view button */}
                <button
                  onClick={centerGraphView}
                  className="tooltip p-2 rounded-full bg-gray-100 text-gray-700 hover:bg-gray-200 flex items-center justify-center shadow hover:shadow-md transition-all duration-150 transform hover:scale-105 active:scale-95"
                  title="Center View"
                >
                  <FaCompass size={16} />
                </button>
                
                {/* Add node button */}
                <button 
                  onClick={() => {
                    setShowNodeForm(true);
                  }}
                  className="tooltip p-2 rounded-full bg-blue-500 text-white hover:bg-blue-600 flex items-center justify-center shadow hover:shadow-md transition-all duration-150 transform hover:scale-105 active:scale-95"
                  title="Add New Node"
                >
                  <FaPlus size={16} />
                </button>
              </div>
            </div>
            
            {/* Graph content */}
            {graphLoading ? (
              <div className="flex justify-center items-center h-[300px] sm:h-[400px] lg:h-[500px]">
                <p className="text-gray-600">Loading graph data...</p>
              </div>
            ) : graphNodes.length === 0 ? (
              <div className="flex flex-col justify-center items-center h-[300px] sm:h-[400px] lg:h-[500px]">
                <p className="text-gray-600 mb-4">No graph data available for this thread.</p>
                <p className="text-gray-500 mb-4">Use the <FaPlus className="inline mx-1" size={12} /> button above to create your first node.</p>
              </div>
            ) : (
              <div className="h-[300px] sm:h-[400px] lg:h-[500px]">
                <GraphVisualization 
                  nodes={graphNodes} 
                  edges={graphEdges} 
                  onNodePositionChange={handleNodePositionChange}
                  onNodeDelete={handleDeleteNode}
                  onNodeEdit={handleEditNode}
                  onNodeDetails={handleViewNodeDetails}
                  onConnectionChange={handleConnectionChange}
                  onEdgeEdit={handleEditEdge}
                  interactionMode={interactionMode}
                  highlightedNodeId={highlightedNodeId}
                  ref={graphRef}
                />
                
                {/* Mode instructions */}
                <div className={`mt-2 px-3 py-2 text-xs rounded-md ${
                  interactionMode === 'move' 
                    ? 'bg-blue-50 text-blue-700 border border-blue-200' 
                    : 'bg-green-50 text-green-700 border border-green-200'
                }`}>
                  {highlightedNodeId ? (
                    <div className="flex justify-between items-center">
                      <p><strong>Node Highlighted:</strong> {graphNodes.find(n => n.id === highlightedNodeId)?.label || 'Selected node'}</p>
                      <button 
                        className="text-gray-500 hover:text-gray-700"
                        onClick={() => setHighlightedNodeId(null)}
                      >
                        <FaTimes size={10} />
                      </button>
                    </div>
                  ) : interactionMode === 'move' ? (
                    <p><strong>Move Mode:</strong> Click nodes to view details. Drag to reposition. Double-click to edit.</p>
                  ) : (
                    <p><strong>Connect Mode:</strong> Click a source node, then click a target node to create a connection between them.</p>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
        
        {/* Comment Section */}
        {thread && <CommentSection threadId={thread.id} threadAuthorId={thread.authorId} />}
      </div>
    );
  };

  const renderNodeForm = () => (
    <div className="fixed inset-0 bg-black/30 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">Create New Node</h3>
          <button 
            onClick={() => setShowNodeForm(false)}
            className="text-gray-500 hover:text-gray-700"
          >
            <FaTimes />
          </button>
        </div>
        
        {formError && (
          <div className="mb-6 p-3 bg-red-50 border-l-4 border-red-500 rounded-md shadow-sm text-red-700 animate-fade-in">
            <div className="flex items-center">
              <div className="text-red-500 mr-3">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <p className="text-sm font-medium">{formError}</p>
            </div>
          </div>
        )}
        
        <form onSubmit={handleCreateNode}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">Node Label</label>
            <input
              type="text"
              value={nodeLabel}
              onChange={(e) => setNodeLabel(e.target.value)}
              className={`w-full px-3 py-2 border ${formError ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'} rounded-md focus:outline-none focus:ring-2`}
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
  );

  const renderNodeEditForm = () => (
    <div className="fixed inset-0 bg-black/30 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">Edit Node</h3>
          <button 
            onClick={handleCancelNodeEdit}
            className="text-gray-500 hover:text-gray-700"
          >
            <FaTimes />
          </button>
        </div>
        
        {formError && (
          <div className="mb-6 p-3 bg-red-50 border-l-4 border-red-500 rounded-md shadow-sm text-red-700 animate-fade-in">
            <div className="flex items-center">
              <div className="text-red-500 mr-3">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <p className="text-sm font-medium">{formError}</p>
            </div>
          </div>
        )}
        
        <form onSubmit={handleSubmitNodeEdit}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">Node Label</label>
            <input
              type="text"
              value={editNodeLabel}
              onChange={(e) => setEditNodeLabel(e.target.value)}
              className={`w-full px-3 py-2 border ${formError ? 'border-red-300 focus:ring-red-500 focus:border-red-500' : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'} rounded-md focus:outline-none focus:ring-2`}
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
  );

  return (
    <MainLayout>
      <div className="px-2 sm:px-4">
        {renderContent()}
        
        {/* Node Create Modal */}
        {showNodeForm && renderNodeForm()}
        
        {/* Node Edit Modal */}
        {showNodeEditModal && renderNodeEditForm()}
        
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
      </div>
    </MainLayout>
  );
};

export default ThreadDetail; 