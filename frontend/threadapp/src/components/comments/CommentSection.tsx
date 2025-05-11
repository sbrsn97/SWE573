import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaReply, FaThumbsUp, FaThumbsDown, FaRegThumbsUp, FaRegThumbsDown, FaTimes, FaComments, FaUser, FaTrash, FaTrashAlt } from 'react-icons/fa';
import { Comment } from '../../interfaces/Comment';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';

interface CommentSectionProps {
  threadId: number;
}

interface CreateCommentDTO {
  content: string;
  threadId: number;
  parentId?: number;
  referencedNodeIds?: number[];
}

interface CommentCounts {
  [key: number]: number;
}

interface VoteStatus {
  hasVoted: boolean;
  voteType: string | null;
  voteCount: number;
}

const CommentSection = ({ threadId }: CommentSectionProps) => {
  const [parentComments, setParentComments] = useState<Comment[]>([]);
  const [childComments, setChildComments] = useState<Record<number, Comment[]>>({});
  const [childCounts, setChildCounts] = useState<CommentCounts>({});
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [childrenLoading, setChildrenLoading] = useState<Record<number, boolean>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [userRole, setUserRole] = useState<string>('USER');
  const [votingInProgress, setVotingInProgress] = useState<Record<number, boolean>>({});
  const [deletingComments, setDeletingComments] = useState<Record<number, boolean>>({});
  // Reply state
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchParentComments();
    fetchCurrentUser();
  }, [threadId]);

  const fetchCurrentUser = async () => {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.users.me);
      if (response.ok) {
        const { data } = await response.json();
        setCurrentUserId(data.id);
        setUserRole(data.role);
      }
    } catch (err) {
      console.error('Error fetching current user:', err);
    }
  };

  // Fetch vote status for a single comment
  const fetchCommentVoteStatus = async (commentId: number) => {
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.votes.commentVoteStatus(commentId));
      
      if (response.ok) {
        const { data } = await response.json();
        return data as VoteStatus;
      }
    } catch (err) {
      console.error(`Error fetching vote status for comment ${commentId}:`, err);
    }
    return null;
  };

  // Update comments with vote status information
  const updateCommentsWithVoteStatus = async (comments: Comment[]): Promise<Comment[]> => {
    const updatedComments = [...comments];
    
    const voteStatuses = await Promise.all(
      updatedComments.map(comment => fetchCommentVoteStatus(comment.id))
    );
    
    return updatedComments.map((comment, index) => {
      const voteStatus = voteStatuses[index];
      if (voteStatus) {
        return {
          ...comment,
          hasUserVoted: voteStatus.hasVoted,
          userVoteType: voteStatus.voteType
        };
      }
      return comment;
    });
  };

  // Vote on a comment
  const handleVote = async (commentId: number, isUpvote: boolean, isParent: boolean) => {
    if (!currentUserId) {
      setError('You must be logged in to vote');
      return;
    }
    
    setVotingInProgress(prev => ({ ...prev, [commentId]: true }));
    setError(null);
    
    try {
      const comment = isParent 
        ? parentComments.find(c => c.id === commentId)
        : childComments[isParent ? commentId : (parentComments.find(c => 
            childComments[c.id]?.some(child => child.id === commentId)
          )?.id || 0)]?.find(c => c.id === commentId);
      
      if (!comment) {
        throw new Error('Comment not found');
      }
      
      // If already voted the same way, remove the vote
      if (comment.hasUserVoted && 
          ((isUpvote && comment.userVoteType === 'UPVOTE') || 
           (!isUpvote && comment.userVoteType === 'DOWNVOTE'))) {
        // Remove vote
        const response = await fetchWithAuth(API_ENDPOINTS.votes.removeCommentVote(commentId), {
          method: 'DELETE'
        });
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          const errorData = await response.json();
          throw new Error(errorData.message || 'Failed to remove vote');
        }
        
        const { data } = await response.json();
        
        updateCommentAfterVote(commentId, null, data.voteCount, isParent);
      } else {
        // Add or change vote - using query parameter instead of request body
        const url = `${API_ENDPOINTS.votes.commentVote(commentId)}?isUpvote=${isUpvote}`;
        const response = await fetchWithAuth(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          const errorData = await response.json();
          throw new Error(errorData.message || 'Failed to vote');
        }
        
        const { data } = await response.json();
        
        updateCommentAfterVote(
          commentId, 
          isUpvote ? 'UPVOTE' : 'DOWNVOTE', 
          data.voteCount,
          isParent
        );
      }
    } catch (err) {
      console.error('Error voting:', err);
      setError(err instanceof Error ? err.message : 'An error occurred while voting');
    } finally {
      setVotingInProgress(prev => ({ ...prev, [commentId]: false }));
    }
  };

  // Update comment data after voting
  const updateCommentAfterVote = (
    commentId: number, 
    voteType: string | null, 
    newVoteCount: number,
    isParent: boolean
  ) => {
    if (isParent) {
      setParentComments(prev => prev.map(comment => {
        if (comment.id === commentId) {
          // Calculate new upvote/downvote counts
          let upvotes = comment.upvoteCount;
          let downvotes = comment.downvoteCount;
          
          // If removing a vote
          if (voteType === null) {
            if (comment.userVoteType === 'UPVOTE') {
              upvotes = Math.max(0, upvotes - 1);
            } else if (comment.userVoteType === 'DOWNVOTE') {
              downvotes = Math.max(0, downvotes - 1);
            }
          } 
          // If adding or changing a vote
          else {
            // Remove previous vote if any
            if (comment.userVoteType === 'UPVOTE') {
              upvotes = Math.max(0, upvotes - 1);
            } else if (comment.userVoteType === 'DOWNVOTE') {
              downvotes = Math.max(0, downvotes - 1);
            }
            
            // Add new vote
            if (voteType === 'UPVOTE') {
              upvotes += 1;
            } else if (voteType === 'DOWNVOTE') {
              downvotes += 1;
            }
          }
          
          return {
            ...comment,
            hasUserVoted: voteType !== null,
            userVoteType: voteType,
            upvoteCount: upvotes,
            downvoteCount: downvotes
          };
        }
        return comment;
      }));
    } else {
      // Find which parent contains this child comment
      const parentId = Object.keys(childComments).find(
        key => childComments[Number(key)].some(child => child.id === commentId)
      );
      
      if (parentId) {
        setChildComments(prev => ({
          ...prev,
          [Number(parentId)]: prev[Number(parentId)].map(comment => {
            if (comment.id === commentId) {
              // Calculate new upvote/downvote counts
              let upvotes = comment.upvoteCount;
              let downvotes = comment.downvoteCount;
              
              // If removing a vote
              if (voteType === null) {
                if (comment.userVoteType === 'UPVOTE') {
                  upvotes = Math.max(0, upvotes - 1);
                } else if (comment.userVoteType === 'DOWNVOTE') {
                  downvotes = Math.max(0, downvotes - 1);
                }
              } 
              // If adding or changing a vote
              else {
                // Remove previous vote if any
                if (comment.userVoteType === 'UPVOTE') {
                  upvotes = Math.max(0, upvotes - 1);
                } else if (comment.userVoteType === 'DOWNVOTE') {
                  downvotes = Math.max(0, downvotes - 1);
                }
                
                // Add new vote
                if (voteType === 'UPVOTE') {
                  upvotes += 1;
                } else if (voteType === 'DOWNVOTE') {
                  downvotes += 1;
                }
              }
              
              return {
                ...comment,
                hasUserVoted: voteType !== null,
                userVoteType: voteType,
                upvoteCount: upvotes,
                downvoteCount: downvotes
              };
            }
            return comment;
          })
        }));
      }
    }
  };

  const fetchParentComments = async () => {
    setLoading(true);
    setError(null);

    try {
      // Using the new API to fetch only parent comments
      const response = await fetchWithAuth(API_ENDPOINTS.comments.getParentsByThread(threadId));
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        const errorData = await response.json();
        setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
        setLoading(false);
        return;
      }

      const { data } = await response.json();
      // Filter to only include active comments
      const activeComments = data ? data.filter((comment: Comment) => comment.active) : [];
      
      // Add vote status information
      const commentsWithVoteStatus = await updateCommentsWithVoteStatus(activeComments);
      setParentComments(commentsWithVoteStatus);
      
      // Get initial counts for all parent comments
      fetchChildCountsForAllParents(activeComments.map((comment: Comment) => comment.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while fetching comments');
    } finally {
      setLoading(false);
    }
  };

  const fetchChildCountsForAllParents = async (parentIds: number[]) => {
    try {
      const counts: CommentCounts = {};
      
      // For each parent, get the count of child comments
      await Promise.all(parentIds.map(async (parentId) => {
        const response = await fetchWithAuth(API_ENDPOINTS.comments.getChildrenByParent(parentId));
        
        if (response.ok) {
          const { data } = await response.json();
          // Only count active comments
          const activeChildCount = data ? data.filter((comment: Comment) => comment.active).length : 0;
          counts[parentId] = activeChildCount;
        }
      }));
      
      setChildCounts(counts);
    } catch (err) {
      console.error('Error fetching child counts:', err);
    }
  };

  const fetchChildComments = async (parentId: number) => {
    // Skip if we already have the children loaded
    if (childComments[parentId]) return;
    
    setChildrenLoading(prev => ({ ...prev, [parentId]: true }));

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.comments.getChildrenByParent(parentId));
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        console.error(`Error fetching child comments: ${response.status}`);
        setChildrenLoading(prev => ({ ...prev, [parentId]: false }));
        return;
      }

      const { data } = await response.json();
      // Filter to only include active comments
      const activeChildren = data ? data.filter((comment: Comment) => comment.active) : [];
      
      // Add vote status information
      const childrenWithVoteStatus = await updateCommentsWithVoteStatus(activeChildren);
      
      setChildComments(prev => ({ 
        ...prev, 
        [parentId]: childrenWithVoteStatus 
      }));
      
      // Update the count to match the actual number of active children
      setChildCounts(prev => ({
        ...prev,
        [parentId]: childrenWithVoteStatus.length
      }));
    } catch (err) {
      console.error('Error fetching child comments:', err);
    } finally {
      setChildrenLoading(prev => ({ ...prev, [parentId]: false }));
    }
  };

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!newComment.trim()) return;
    
    setSubmitting(true);
    setError(null);

    const createCommentDTO: CreateCommentDTO = {
      content: newComment,
      threadId: threadId
    };

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.comments.create, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(createCommentDTO)
      });

      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        const errorData = await response.json();
        setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
        setSubmitting(false);
        return;
      }

      // After successful submission, clear the input and refresh comments
      setNewComment('');
      fetchParentComments();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while submitting comment');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitReply = async (e: React.FormEvent, parentId: number) => {
    e.preventDefault();
    
    if (!replyContent.trim() || !parentId) return;
    
    setSubmitting(true);
    setError(null);

    const createCommentDTO: CreateCommentDTO = {
      content: replyContent,
      threadId: threadId,
      parentId: parentId
    };

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.comments.create, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(createCommentDTO)
      });

      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        const errorData = await response.json();
        setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
        setSubmitting(false);
        return;
      }

      // Get the new comment from the response
      const { data } = await response.json();
      
      // Update the child comments for this parent
      setChildComments(prev => {
        const existingChildren = prev[parentId] || [];
        return {
          ...prev,
          [parentId]: [...existingChildren, data]
        }; 
      });
      
      // Update the child count for this parent
      setChildCounts(prev => ({
        ...prev,
        [parentId]: (prev[parentId] || 0) + 1
      }));

      // Clear form and reply state
      setReplyContent('');
      setReplyingTo(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while submitting reply');
    } finally {
      setSubmitting(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  // Load child comments when viewing a parent comment that has children
  const handleViewReplies = (parentId: number) => {
    fetchChildComments(parentId);
  };
  
  // Hide child comments for a parent
  const handleHideReplies = (parentId: number) => {
    setChildComments(prev => {
      const newState = { ...prev };
      delete newState[parentId];
      return newState;
    });
  };

  // Handle soft delete comment
  const handleDeleteComment = async (commentId: number, isParent: boolean) => {
    if (!window.confirm("Are you sure you want to delete this comment?")) {
      return;
    }

    setDeletingComments(prev => ({ ...prev, [commentId]: true }));
    setError(null);

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.comments.delete(commentId), {
        method: 'DELETE'
      });

      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        const errorData = await response.json();
        setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
        return;
      }

      // If it was a parent comment, remove it from the list
      if (isParent) {
        setParentComments(prev => prev.filter(comment => comment.id !== commentId));
        // Remove any child comments
        setChildComments(prev => {
          const newState = { ...prev };
          delete newState[commentId];
          return newState;
        });
      } else {
        // If it was a child comment, find which parent it belongs to
        const parentId = Object.keys(childComments).find(
          key => childComments[Number(key)].some(child => child.id === commentId)
        );
        
        if (parentId) {
          // Update the child comments for this parent
          setChildComments(prev => ({
            ...prev,
            [Number(parentId)]: prev[Number(parentId)].filter(comment => comment.id !== commentId)
          }));
          
          // Update the child count for this parent
          setChildCounts(prev => ({
            ...prev,
            [Number(parentId)]: Math.max(0, (prev[Number(parentId)] || 0) - 1)
          }));
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while deleting the comment');
    } finally {
      setDeletingComments(prev => ({ ...prev, [commentId]: false }));
    }
  };

  // Handle hard delete comment (admin only)
  const handleHardDeleteComment = async (commentId: number, isParent: boolean) => {
    if (!window.confirm("Are you sure you want to PERMANENTLY delete this comment? This action cannot be undone.")) {
      return;
    }

    setDeletingComments(prev => ({ ...prev, [commentId]: true }));
    setError(null);

    try {
      const response = await fetchWithAuth(API_ENDPOINTS.comments.hardDelete(commentId), {
        method: 'DELETE'
      });

      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        const errorData = await response.json();
        setError(errorData.message || `Error ${response.status}: ${response.statusText}`);
        return;
      }

      // Same removal logic as soft delete
      if (isParent) {
        setParentComments(prev => prev.filter(comment => comment.id !== commentId));
        setChildComments(prev => {
          const newState = { ...prev };
          delete newState[commentId];
          return newState;
        });
      } else {
        const parentId = Object.keys(childComments).find(
          key => childComments[Number(key)].some(child => child.id === commentId)
        );
        
        if (parentId) {
          setChildComments(prev => ({
            ...prev,
            [Number(parentId)]: prev[Number(parentId)].filter(comment => comment.id !== commentId)
          }));
          
          setChildCounts(prev => ({
            ...prev,
            [Number(parentId)]: Math.max(0, (prev[Number(parentId)] || 0) - 1)
          }));
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred while permanently deleting the comment');
    } finally {
      setDeletingComments(prev => ({ ...prev, [commentId]: false }));
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm p-6 mt-6">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">Comments</h2>
      
      {/* Comment form */}
      <form onSubmit={handleSubmitComment} className="mb-6">
        <div className="mb-3">
          <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Add a comment..."
            rows={3}
            required
          />
        </div>
        <div className="flex justify-end">
          <button
            type="submit"
            className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
            disabled={submitting}
          >
            {submitting ? 'Submitting...' : 'Post Comment'}
          </button>
        </div>
      </form>

      {/* Error display */}
      {error && (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg mb-4">
          {error}
        </div>
      )}

      {/* Comments list */}
      {loading ? (
        <div className="flex justify-center items-center py-6">
          <p className="text-gray-600">Loading...</p>
        </div>
      ) : parentComments.length === 0 ? (
        <div className="text-center py-6 text-gray-500">
          No comments yet. Be the first to comment!
        </div>
      ) : (
        <div className="space-y-6">
          {parentComments.map((comment) => (
            <div 
              key={comment.id} 
              className={`border-b border-gray-200 pb-4 last:border-b-0 rounded-lg p-4 shadow-sm 
                hover:shadow-md transition-all duration-200 ease-in-out ${
                comment.authorId === currentUserId 
                  ? 'bg-blue-50/50' 
                  : 'bg-gray-50/50'
              }`}
            >
              {/* Parent comment */}
              <div className="mb-2">
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center gap-2">
                    <div className={`rounded-full w-8 h-8 flex items-center justify-center text-white ${
                      comment.authorId === currentUserId ? 'bg-blue-500' : 'bg-gray-500'
                    }`}>
                      <FaUser size={14} />
                    </div>
                    <div>
                      <div className="font-medium text-gray-900">
                        <Link 
                          to={`/users/${comment.authorId}`}
                          className="hover:text-blue-600 transition-colors"
                        >
                          {comment.authorUsername}
                        </Link>
                        {comment.authorId === currentUserId && (
                          <span className="ml-2 text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded-full">You</span>
                        )}
                      </div>
                      <div className="text-xs text-gray-500">
                        {formatDate(comment.createdAt)}
                      </div>
                    </div>
                  </div>
                  
                  {/* Delete controls */}
                  <div className="flex gap-2">
                    {/* Show delete button to comment author or any admin */}
                    {(comment.authorId === currentUserId || userRole === 'ADMIN') && (
                      <button
                        className="text-gray-400 hover:text-red-500 transition-colors p-1 rounded hover:bg-gray-100 flex items-center gap-1"
                        onClick={() => handleDeleteComment(comment.id, true)}
                        disabled={deletingComments[comment.id]}
                        title="Delete comment"
                      >
                        <FaTrash size={14} />
                        {userRole === 'ADMIN' && <span className="text-xs">Deactivate</span>}
                      </button>
                    )}
                    
                    {/* Show hard delete button only to admins */}
                    {userRole === 'ADMIN' && (
                      <button
                        className="text-gray-400 hover:text-red-700 transition-colors p-1 rounded hover:bg-gray-100 flex items-center gap-1"
                        onClick={() => handleHardDeleteComment(comment.id, true)}
                        disabled={deletingComments[comment.id]}
                        title="Permanently delete comment"
                      >
                        <FaTrashAlt size={14} />
                        <span className="text-xs">Delete</span>
                      </button>
                    )}
                  </div>
                </div>
                
                <div className="text-gray-700 mb-3 ml-10">
                  {comment.content}
                </div>
                <div className="flex gap-4 items-center ml-10">
                  <div className="flex items-center gap-1">
                    <button 
                      className={`${
                        comment.userVoteType === 'UPVOTE' 
                          ? 'text-blue-600' 
                          : 'text-gray-500 hover:text-blue-600'
                      } transition-colors`}
                      onClick={() => handleVote(comment.id, true, true)}
                      disabled={votingInProgress[comment.id]}
                    >
                      {comment.userVoteType === 'UPVOTE' ? (
                        <FaThumbsUp size={14} />
                      ) : (
                        <FaRegThumbsUp size={14} />
                      )}
                    </button>
                    <span className="text-xs text-gray-500">{comment.upvoteCount}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <button 
                      className={`${
                        comment.userVoteType === 'DOWNVOTE' 
                          ? 'text-red-600' 
                          : 'text-gray-500 hover:text-red-600'
                      } transition-colors`}
                      onClick={() => handleVote(comment.id, false, true)}
                      disabled={votingInProgress[comment.id]}
                    >
                      {comment.userVoteType === 'DOWNVOTE' ? (
                        <FaThumbsDown size={14} />
                      ) : (
                        <FaRegThumbsDown size={14} />
                      )}
                    </button>
                    <span className="text-xs text-gray-500">{comment.downvoteCount}</span>
                  </div>
                  <button 
                    className="text-gray-500 hover:text-blue-600 text-sm flex items-center gap-1 transition-colors"
                    onClick={() => setReplyingTo(comment.id)}
                  >
                    <FaReply size={14} />
                    <span>Reply</span>
                  </button>
                  {/* View replies button if not already loaded and there are replies */}
                  {!childComments[comment.id] && childCounts[comment.id] > 0 && (
                    <button 
                      className="text-gray-500 hover:text-blue-600 text-sm flex items-center gap-1 transition-colors"
                      onClick={() => handleViewReplies(comment.id)}
                    >
                      <FaComments size={14} />
                      <span>View Replies ({childCounts[comment.id]})</span>
                    </button>
                  )}
                  {/* Hide replies button if replies are loaded */}
                  {childComments[comment.id] && childComments[comment.id].length > 0 && (
                    <button 
                      className="text-gray-500 hover:text-blue-600 text-sm flex items-center gap-1 transition-colors"
                      onClick={() => handleHideReplies(comment.id)}
                    >
                      <FaComments size={14} />
                      <span>Hide Replies ({childComments[comment.id].length})</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Reply form */}
              {replyingTo === comment.id && (
                <div className="ml-16 mt-3 mb-4 border-l-2 border-gray-200 pl-4 animate-fadeIn">
                  <form onSubmit={(e) => handleSubmitReply(e, comment.id)} className="space-y-3">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-sm font-medium text-gray-700">Reply to {comment.authorUsername}</span>
                      <button 
                        type="button" 
                        className="text-gray-500 hover:text-gray-700 transition-colors"
                        onClick={() => setReplyingTo(null)}
                      >
                        <FaTimes size={14} />
                      </button>
                    </div>
                    <textarea
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Write your reply..."
                      rows={2}
                      required
                    />
                    <div className="flex justify-end">
                      <button
                        type="submit"
                        className="px-3 py-1.5 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
                        disabled={submitting}
                      >
                        {submitting ? 'Sending...' : 'Reply'}
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {/* Child comments */}
              {childrenLoading[comment.id] ? (
                <div className="ml-16 pl-4 py-2 text-sm text-gray-500 animate-pulse">
                  Loading replies...
                </div>
              ) : childComments[comment.id] && childComments[comment.id].length > 0 ? (
                <div className="mt-3 ml-16 space-y-3 border-l-2 border-gray-200 pl-4 animate-fadeIn">
                  {childComments[comment.id].map(childComment => (
                    <div 
                      key={childComment.id} 
                      className={`p-3 rounded-lg shadow-sm hover:shadow transition-all duration-200 ${
                        childComment.authorId === currentUserId 
                          ? 'bg-blue-50/70' 
                          : 'bg-gray-50/70'
                      }`}
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex items-center gap-2">
                          <div className={`rounded-full w-6 h-6 flex items-center justify-center text-white ${
                            childComment.authorId === currentUserId ? 'bg-blue-500' : 'bg-gray-500'
                          }`}>
                            <FaUser size={10} />
                          </div>
                          <div>
                            <div className="font-medium text-gray-900 text-sm">
                              <Link 
                                to={`/users/${childComment.authorId}`}
                                className="hover:text-blue-600 transition-colors"
                              >
                                {childComment.authorUsername}
                              </Link>
                              {childComment.authorId === currentUserId && (
                                <span className="ml-2 text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded-full text-[10px]">You</span>
                              )}
                            </div>
                            <div className="text-xs text-gray-500">
                              {formatDate(childComment.createdAt)}
                            </div>
                          </div>
                        </div>
                        
                        {/* Delete controls for child comments */}
                        <div className="flex gap-2">
                          {/* Show delete button to comment author or any admin */}
                          {(childComment.authorId === currentUserId || userRole === 'ADMIN') && (
                            <button
                              className="text-gray-400 hover:text-red-500 transition-colors p-1 rounded hover:bg-gray-100 flex items-center gap-1"
                              onClick={() => handleDeleteComment(childComment.id, false)}
                              disabled={deletingComments[childComment.id]}
                              title="Delete comment"
                            >
                              <FaTrash size={12} />
                              {userRole === 'ADMIN' && <span className="text-xs">Deactivate</span>}
                            </button>
                          )}
                          
                          {/* Show hard delete button only to admins */}
                          {userRole === 'ADMIN' && (
                            <button
                              className="text-gray-400 hover:text-red-700 transition-colors p-1 rounded hover:bg-gray-100 flex items-center gap-1"
                              onClick={() => handleHardDeleteComment(childComment.id, false)}
                              disabled={deletingComments[childComment.id]}
                              title="Permanently delete comment"
                            >
                              <FaTrashAlt size={12} />
                              <span className="text-xs">Delete</span>
                            </button>
                          )}
                        </div>
                      </div>
                      
                      <div className="text-gray-700 mb-2 ml-8 text-sm">
                        {childComment.content}
                      </div>
                      
                      <div className="flex gap-4 items-center ml-8">
                        <div className="flex items-center gap-1">
                          <button 
                            className={`${
                              childComment.userVoteType === 'UPVOTE' 
                                ? 'text-blue-600' 
                                : 'text-gray-500 hover:text-blue-600'
                            } transition-colors`}
                            onClick={() => handleVote(childComment.id, true, false)}
                            disabled={votingInProgress[childComment.id]}
                          >
                            {childComment.userVoteType === 'UPVOTE' ? (
                              <FaThumbsUp size={12} />
                            ) : (
                              <FaRegThumbsUp size={12} />
                            )}
                          </button>
                          <span className="text-xs text-gray-500">{childComment.upvoteCount}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <button 
                            className={`${
                              childComment.userVoteType === 'DOWNVOTE' 
                                ? 'text-red-600' 
                                : 'text-gray-500 hover:text-red-600'
                            } transition-colors`}
                            onClick={() => handleVote(childComment.id, false, false)}
                            disabled={votingInProgress[childComment.id]}
                          >
                            {childComment.userVoteType === 'DOWNVOTE' ? (
                              <FaThumbsDown size={12} />
                            ) : (
                              <FaRegThumbsDown size={12} />
                            )}
                          </button>
                          <span className="text-xs text-gray-500">{childComment.downvoteCount}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : childComments[comment.id] && childComments[comment.id].length === 0 ? (
                <div className="ml-16 pl-4 py-2 text-sm text-gray-500 italic">
                  No replies yet
                </div>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

// Add animation classes to your CSS or tailwind.config.js
export default CommentSection; 