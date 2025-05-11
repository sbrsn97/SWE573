import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { FaArrowLeft, FaCalendar, FaUser, FaTag, FaEdit, FaTrash, FaPlus, FaThumbsUp, FaThumbsDown, FaUserPlus, FaUserMinus } from 'react-icons/fa';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';
import MainLayout from '../layout/MainLayout';

interface User {
  id: number;
  username: string;
}

interface ThreadHistoryEntry {
  id: number;
  threadId: number;
  threadTitle: string;
  user: User;
  actionType: string;
  entityType: string;
  entityId: number | null;
  beforeState: string | null;
  afterState: string | null;
  description: string;
  createdAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  code?: string;
}

const ThreadHistory = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [history, setHistory] = useState<ThreadHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [threadTitle, setThreadTitle] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    fetchThreadHistory();
  }, [id, currentPage, pageSize]);

  const fetchThreadHistory = async () => {
    if (!id) return;
    setLoading(true);
    
    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.threads.history.getPaginated(Number(id), currentPage, pageSize)
      );
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        
        const errorData = await response.json();
        setError(errorData.message || 'Failed to fetch thread history');
        setLoading(false);
        return;
      }
      
      const data: ApiResponse<{content: ThreadHistoryEntry[], totalPages: number}> = await response.json();
      
      if (data.success) {
        setHistory(data.data.content);
        setTotalPages(data.data.totalPages);
        
        // If we have history entries, set the thread title from the first entry
        if (data.data.content.length > 0) {
          setThreadTitle(data.data.content[0].threadTitle);
        }
      } else {
        setError(data.message || 'Failed to fetch thread history');
      }
    } catch (err) {
      console.error('Error fetching thread history:', err);
      setError('An error occurred while fetching the thread history');
    } finally {
      setLoading(false);
    }
  };

  const getActionIcon = (actionType: string) => {
    switch (actionType) {
      case 'CREATE':
        return <FaPlus className="text-green-500" />;
      case 'UPDATE':
        return <FaEdit className="text-blue-500" />;
      case 'DELETE':
        return <FaTrash className="text-red-500" />;
      case 'FOLLOW':
        return <FaUserPlus className="text-purple-500" />;
      case 'UNFOLLOW':
        return <FaUserMinus className="text-gray-500" />;
      case 'UPVOTE':
        return <FaThumbsUp className="text-blue-500" />;
      case 'DOWNVOTE':
        return <FaThumbsDown className="text-orange-500" />;
      case 'REMOVE_VOTE':
        return <FaThumbsDown className="text-gray-500" />;
      case 'ADD_TAG':
        return <FaTag className="text-teal-500" />;
      case 'REMOVE_TAG':
        return <FaTag className="text-gray-500" />;
      default:
        return <FaEdit className="text-gray-500" />;
    }
  };

  const getEntityIcon = (entityType: string) => {
    switch (entityType) {
      case 'THREAD':
        return <span className="bg-blue-100 text-blue-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">Thread</span>;
      case 'NODE':
        return <span className="bg-green-100 text-green-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">Node</span>;
      case 'EDGE':
        return <span className="bg-purple-100 text-purple-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">Edge</span>;
      case 'COMMENT':
        return <span className="bg-yellow-100 text-yellow-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">Comment</span>;
      case 'TAG':
        return <span className="bg-teal-100 text-teal-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">Tag</span>;
      default:
        return <span className="bg-gray-100 text-gray-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded">{entityType}</span>;
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;
    
    return (
      <div className="flex justify-center mt-6">
        <div className="inline-flex items-center -space-x-px">
          <button
            onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
            disabled={currentPage === 0}
            className={`px-3 py-2 ml-0 leading-tight text-gray-500 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-100 hover:text-gray-700 ${currentPage === 0 ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Previous
          </button>
          
          {Array.from({ length: Math.min(5, totalPages) }).map((_, i) => {
            // Calculate pages to show
            let pageToShow;
            if (totalPages <= 5) {
              pageToShow = i;
            } else if (currentPage < 3) {
              pageToShow = i;
            } else if (currentPage > totalPages - 3) {
              pageToShow = totalPages - 5 + i;
            } else {
              pageToShow = currentPage - 2 + i;
            }
            
            if (pageToShow >= 0 && pageToShow < totalPages) {
              return (
                <button
                  key={pageToShow}
                  onClick={() => setCurrentPage(pageToShow)}
                  className={`px-3 py-2 leading-tight ${currentPage === pageToShow ? 'text-blue-600 bg-blue-50 border border-blue-300' : 'text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700'}`}
                >
                  {pageToShow + 1}
                </button>
              );
            }
            return null;
          })}
          
          <button
            onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
            disabled={currentPage === totalPages - 1}
            className={`px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-100 hover:text-gray-700 ${currentPage === totalPages - 1 ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            Next
          </button>
        </div>
      </div>
    );
  };

  return (
    <MainLayout>
      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center mb-6">
          <button
            onClick={() => navigate(`/threads/${id}`)}
            className="mr-4 p-2 rounded-full hover:bg-gray-100"
          >
            <FaArrowLeft className="text-gray-600" />
          </button>
          
          <h1 className="text-2xl font-bold text-gray-800">
            Thread History: {threadTitle || `#${id}`}
          </h1>
        </div>

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : error ? (
          <div className="bg-red-50 border border-red-200 text-red-800 rounded-lg p-4 mb-6">
            {error}
          </div>
        ) : history.length === 0 ? (
          <div className="bg-gray-50 border border-gray-200 text-gray-800 rounded-lg p-4 mb-6">
            No history entries found for this thread.
          </div>
        ) : (
          <>
            <div className="bg-white shadow-sm rounded-lg overflow-hidden">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Action
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Entity
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        User
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Description
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        Date
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {history.map((entry) => (
                      <tr key={entry.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <div className="mr-2">
                              {getActionIcon(entry.actionType)}
                            </div>
                            <span className="text-sm text-gray-700">{entry.actionType.replace('_', ' ')}</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {getEntityIcon(entry.entityType)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <Link 
                            to={`/users/${entry.user.id}`}
                            className="text-sm text-blue-600 hover:text-blue-800"
                          >
                            @{entry.user.username}
                          </Link>
                        </td>
                        <td className="px-6 py-4">
                          <p className="text-sm text-gray-700">{entry.description}</p>
                          {entry.beforeState && entry.afterState && (
                            <div className="mt-1 text-xs">
                              <div className="flex flex-col gap-1">
                                <div className="text-red-500 bg-red-50 p-1 rounded-sm line-through">
                                  {entry.beforeState}
                                </div>
                                <div className="text-green-500 bg-green-50 p-1 rounded-sm">
                                  {entry.afterState}
                                </div>
                              </div>
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                          <div className="flex items-center">
                            <FaCalendar className="text-gray-400 mr-1" size={12} />
                            {formatDate(entry.createdAt)}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
            
            {renderPagination()}
          </>
        )}
      </div>
    </MainLayout>
  );
};

export default ThreadHistory; 