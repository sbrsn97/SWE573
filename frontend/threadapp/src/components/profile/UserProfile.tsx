import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, Link, useSearchParams } from 'react-router-dom';
import { Avatar } from 'primereact/avatar';
import { Button } from 'primereact/button';
import { Calendar } from 'primereact/calendar';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Toast } from 'primereact/toast';
import { Dialog } from 'primereact/dialog';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';
import { FaUserPlus, FaUserMinus, FaPencilAlt, FaTags, FaChevronLeft, FaChevronRight, FaFilter, FaSort, FaCalendarAlt } from 'react-icons/fa';
import Tag from '../tags/Tag';
import 'primeicons/primeicons.css';
import ThreadCard from '../threads/ThreadCard';
import { isProfanityError, formatProfanityError, ProfanityErrorMessage } from '../../utils/errorUtils';

interface User {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  role: string;
  initials?: string;
  bio?: string;
  location?: string;
  profession?: string;
  birthDate?: string;
  createdAt?: string;
  updatedAt?: string;
  followerIds?: number[];
  followingIds?: number[];
  tags?: Array<{
    id: number;
    label: string;
    description: string;
    colorCodeString: string;
    wikidataEntityId: string;
  }>;
}

interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface EditableField {
  name: 'bio' | 'location' | 'profession' | 'birthDate';
  value: string;
}

interface Thread {
  id: number;
  title: string;
  description: string | null;
  authorId: number;
  upvoteCount: number;
  downvoteCount: number;
  createdAt: string;
  updatedAt: string;
  tags: Array<{
    id: number;
    label: string;
    description: string;
    colorCodeString: string;
    wikidataEntityId: string;
  }>;
  followerIds?: number[];
}

interface PaginatedResponse {
  content: Thread[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

const UserProfile = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Edit mode state
  const [editingField, setEditingField] = useState<EditableField | null>(null);
  const [updating, setUpdating] = useState(false);
  const toast = useRef<Toast>(null);
  
  // Tags state
  const [showTagsDialog, setShowTagsDialog] = useState(false);
  const [availableTags, setAvailableTags] = useState<Tag[]>([]);
  const [selectedTags, setSelectedTags] = useState<number[]>([]);
  const [loadingTags, setLoadingTags] = useState(false);
  const [tagSearchQuery, setTagSearchQuery] = useState('');

  // Thread state
  const [userThreads, setUserThreads] = useState<Thread[]>([]);
  const [allUserThreads, setAllUserThreads] = useState<Thread[]>([]);
  const [loadingThreads, setLoadingThreads] = useState(false);
  const [threadsError, setThreadsError] = useState<string | null>(null);
  const [totalPages, setTotalPages] = useState(1);
  
  // Thread filtering and pagination
  const pageSize = 10; // Number of threads per page
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  
  // Thread filters
  const [threadFilterTags, setThreadFilterTags] = useState<number[]>([]);
  const [dateFilter, setDateFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<string>('newest');
  const [showFilters, setShowFilters] = useState(false);

  const formatDate = (dateString?: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('tr-TR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };
  
  // Helper to safely get user data from API response
  const extractUserData = (result: any): User | null => {
    try {
      // Handle different response structures
      const userData = result.data?.data || result.data;
      
      if (!userData || typeof userData !== 'object') {
        console.error('Invalid user data format:', result);
        return null;
      }
      
      return {
        ...userData,
        initials: userData.firstName.charAt(0) + userData.lastName.charAt(0)
      };
    } catch (err) {
      console.error('Error extracting user data:', err);
      return null;
    }
  };

  // Fetch all available tags
  const fetchAvailableTags = async () => {
    setLoadingTags(true);
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.tags.getAll);
      if (response.ok) {
        const result = await response.json();
        if (result && result.data) {
          setAvailableTags(result.data);
        }
      } else {
        console.error('Failed to fetch tags');
      }
    } catch (err) {
      console.error('Error fetching tags:', err);
    } finally {
      setLoadingTags(false);
    }
  };

  // Filter tags based on search query
  const getFilteredTags = () => {
    if (tagSearchQuery.trim() === '') {
      return availableTags;
    } else {
      return availableTags.filter(tag => 
        tag.label.toLowerCase().includes(tagSearchQuery.toLowerCase())
      );
    }
  };

  // Update user tags
  const updateUserTags = async () => {
    if (!user || !isOwnProfile) return;
    
    setUpdating(true);
    try {
      const updatePayload = {
        ...user,
        tagIds: selectedTags
      };

      // Remove non-API fields
      delete updatePayload.initials;
      delete updatePayload.followerIds;
      delete updatePayload.followingIds;
      delete updatePayload.tags;

      const response = await fetchWithAuth(API_ENDPOINTS.users.update(user.id), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatePayload)
      });

      if (response.ok) {
        const result = await response.json();
        const updatedUser = extractUserData(result);
        
        if (!updatedUser) {
          toast.current?.show({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to parse updated user data',
            life: 3000
          });
          return;
        }
        
        setUser(updatedUser);
        
        toast.current?.show({
          severity: 'success',
          summary: 'Success',
          detail: 'Tags updated successfully',
          life: 3000
        });

        setShowTagsDialog(false);
      } else {
        await handleAuthError(response, navigate);
        toast.current?.show({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to update tags',
          life: 3000
        });
      }
    } catch (error) {
      console.error('Error updating user tags:', error);
      toast.current?.show({
        severity: 'error',
        summary: 'Error',
        detail: error instanceof Error ? error.message : 'Failed to update tags',
        life: 5000
      });
    } finally {
      setUpdating(false);
    }
  };

  // Handle tag selection
  const handleTagSelection = (tagId: number) => {
    if (selectedTags.includes(tagId)) {
      setSelectedTags(selectedTags.filter(id => id !== tagId));
    } else {
      setSelectedTags([...selectedTags, tagId]);
    }
  };

  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const response = await fetchWithAuth(API_ENDPOINTS.users.me);
        if (response.ok) {
          const result = await response.json();
          const userData = extractUserData(result);
        
          if (!userData) {
            setError('Failed to parse user data');
          return;
        }
        
          setCurrentUser(userData);
          
          // If no ID was provided in URL or ID matches current user, display own profile
          if (!id || id === 'me' || id === userData.id.toString()) {
            setUser(userData);
            setIsOwnProfile(true);
            setFollowerCount(userData.followerIds?.length || 0);
            setFollowingCount(userData.followingIds?.length || 0);
            
            // Initialize selected tags from user's tags if available
            if (userData.tags) {
              setSelectedTags(userData.tags.map(tag => tag.id));
            }
            
            setLoading(false);
            return;
          }
          
          // Check if current user is following the viewed profile
          if (userData.followingIds) {
            setIsFollowing(userData.followingIds.includes(Number(id)));
          }
        } else {
          await handleAuthError(response, navigate);
          setError('Failed to fetch your profile');
        }
      } catch (err) {
        console.error('Error fetching current user:', err);
        setError('An error occurred while loading data');
      }
    };

    const fetchUserProfile = async () => {
      try {
        if (!id || id === 'me') {
          // Already handled in fetchCurrentUser
          return;
        }
        
        const response = await fetchWithAuth(`${API_ENDPOINTS.users.all}/${id}`);
        if (response.ok) {
          const result = await response.json();
          const userData = extractUserData(result);
          
          if (!userData) {
            setError('Failed to parse user data');
            return;
          }
          
          setUser(userData);
          setFollowerCount(userData.followerIds?.length || 0);
          setFollowingCount(userData.followingIds?.length || 0);
          
          // Initialize selected tags from user's tags if available
          if (userData.tags) {
            setSelectedTags(userData.tags.map(tag => tag.id));
          }
        } else {
          if (response.status === 404) {
            setError('User not found');
          } else {
            await handleAuthError(response, navigate);
            setError('Failed to fetch user profile');
          }
        }
      } catch (err) {
        console.error('Error fetching user profile:', err);
        setError('An error occurred while loading data');
      } finally {
        setLoading(false);
      }
    };
    
    const loadProfileData = async () => {
      setLoading(true);
      setError(null);
      await fetchCurrentUser();
      await fetchUserProfile();
      await fetchAvailableTags();
    };

    loadProfileData();
  }, [id, navigate]);

  const handleFollow = async () => {
    if (!user || !currentUser) return;
    
    try {
      // Use correct endpoint format with two parameters
      const endpoint = isFollowing 
        ? API_ENDPOINTS.users.unfollow(currentUser.id, user.id) 
        : API_ENDPOINTS.users.follow(currentUser.id, user.id);
      
      const response = await fetchWithAuth(endpoint, {
        method: 'POST'
      });
      
      if (response.ok) {
        setIsFollowing(!isFollowing);
        setFollowerCount(prev => isFollowing ? prev - 1 : prev + 1);
        
        // Update UI, but don't use event bus
      } else {
        await handleAuthError(response, navigate);
        toast.current?.show({
          severity: 'error',
          summary: 'Error',
          detail: `Failed to ${isFollowing ? 'unfollow' : 'follow'} user`,
          life: 3000
        });
      }
    } catch (err) {
      console.error('Follow error:', err);
      toast.current?.show({
        severity: 'error',
        summary: 'Error',
        detail: 'An error occurred',
        life: 3000
      });
    }
  };

  // Edit mode functions
  const handleEdit = (name: EditableField['name'], value: string) => {
    setEditingField({ name, value });
  };

  const handleSave = async () => {
    if (!editingField || !user) return;
    
    setUpdating(true);
    try {
      const { name, value } = editingField;
      
      // Create a copy of the user without non-API fields
      const updatePayload = {
        ...user,
        [name]: value
      };
      
      // Remove non-API fields
      delete updatePayload.initials;
      delete updatePayload.followerIds;
      delete updatePayload.followingIds;
      delete updatePayload.tags;
      
      const response = await fetchWithAuth(API_ENDPOINTS.users.update(user.id), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatePayload)
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        const errorMessage = errorData.message || 'Update failed';
        
        // Check if error is profanity-related
        if (isProfanityError(errorMessage)) {
          toast.current?.show({
            severity: 'error',
            summary: 'Inappropriate Content Detected',
            detail: formatProfanityError(errorMessage),
            life: 5000
          });
        } else {
          toast.current?.show({
            severity: 'error',
            summary: 'Error',
            detail: errorMessage,
            life: 3000
          });
        }
        
        if (handleAuthError(response, navigate)) return;
        return;
      }
      
      const result = await response.json();
      const updatedUser = extractUserData(result);
      
      if (!updatedUser) {
        toast.current?.show({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to parse updated user data',
          life: 3000
        });
        return;
      }
      
      setUser(updatedUser);
      setEditingField(null);
      
      toast.current?.show({
        severity: 'success',
        summary: 'Success',
        detail: 'Profile updated successfully',
        life: 3000
      });
    } catch (error) {
      console.error('Error updating user:', error);
      
      toast.current?.show({
        severity: 'error',
        summary: 'Error',
        detail: error instanceof Error ? error.message : 'Failed to update profile',
        life: 3000
      });
    } finally {
      setUpdating(false);
    }
  };

  const handleCancel = () => {
    setEditingField(null);
  };

  const renderEditableFieldCard = (label: string, name: EditableField['name'], value: string) => {
    const isEditing = editingField?.name === name;
    
    // Icons for each field type
    const getFieldIcon = () => {
      switch(name) {
        case 'birthDate': return "pi pi-calendar";
        case 'bio': return "pi pi-info-circle";
        case 'location': return "pi pi-map-marker";
        case 'profession': return "pi pi-briefcase";
        default: return "pi pi-pencil";
      }
    };
    
    return (
      <div className={`bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg ${isEditing ? 'p-4' : 'p-4'} shadow-sm`}>
        {isEditing ? (
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <i className={`${getFieldIcon()} text-blue-500 text-lg`}></i>
                <div className="text-md font-medium text-gray-700">{label}</div>
        </div>
              <div className="flex gap-1">
                <Button 
                  icon="pi pi-check" 
                  rounded 
                  outlined 
                  severity="success" 
                  onClick={handleSave}
                  loading={updating}
                  disabled={updating}
                  size="small"
                />
                <Button 
                  icon="pi pi-times" 
                  rounded 
                  outlined 
                  severity="danger" 
                  onClick={handleCancel}
                  disabled={updating}
                  size="small"
                />
              </div>
            </div>
            
            {name === 'bio' ? (
              <InputTextarea
                value={editingField.value}
                onChange={(e) => setEditingField({ ...editingField, value: e.target.value })}
                rows={3}
                className="w-full text-sm"
                disabled={updating}
              />
            ) : name === 'birthDate' ? (
              <Calendar
                value={editingField.value ? new Date(editingField.value) : null}
                onChange={(e) => {
                  if (e.value) {
                    let dateValue: Date;
                    
                    if (Array.isArray(e.value)) {
                      dateValue = e.value[0] as Date;
                    } else {
                      dateValue = e.value as Date;
                    }
                    
                    const year = dateValue.getFullYear();
                    const month = dateValue.getMonth() + 1; // getMonth is 0-indexed
                    const day = dateValue.getDate();
                    const dateString = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                    setEditingField({ ...editingField, value: dateString });
                  } else {
                    setEditingField({ ...editingField, value: '' });
                  }
                }}
                dateFormat="dd.mm.yy"
                showIcon
                disabled={updating}
                className="text-sm"
              />
            ) : (
              <InputText
                value={editingField.value}
                onChange={(e) => setEditingField({ ...editingField, value: e.target.value })}
                className="w-full text-sm"
                disabled={updating}
              />
            )}
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <i className={`${getFieldIcon()} text-blue-500 text-lg`}></i>
            <div className="flex-1">
              <div className="text-xs text-gray-500 mb-1">{label}</div>
              <div className="font-medium text-gray-800">
                {name === 'birthDate' 
                  ? (value ? formatDate(value) : 'Not specified')
                  : (value || 'Not specified')}
              </div>
            </div>
            {isOwnProfile && (
              <Button 
                icon="pi pi-pencil" 
                rounded
                outlined
                size="small"
                onClick={() => handleEdit(name, value || '')}
                className="p-button-sm"
              />
            )}
          </div>
        )}
      </div>
    );
  };

  // Render tags section with edit capability
  const renderTags = () => {
    const userTags = user?.tags || [];
    
    return (
      <div className="py-1.5 flex items-start gap-3">
        <span className="font-semibold text-gray-700 min-w-[100px] pt-1">Tags:</span>
        <div className="flex-1">
          <div className="flex justify-between items-start">
            <div className="flex flex-wrap gap-1.5 mb-2 flex-1">
              {userTags.length === 0 ? (
                <span className="text-gray-500 text-sm italic">No tags added</span>
              ) : (
                userTags.map(tag => (
                  <Tag key={tag.id} tag={tag} />
                ))
              )}
            </div>
            
            {isOwnProfile && (
            <Button 
                icon="pi pi-pencil"
                rounded
                outlined
                size="small"
                onClick={() => setShowTagsDialog(true)}
                className="p-button-sm"
            />
          )}
        </div>
            </div>
      </div>
    );
  };

  // Render tag selection dialog
  const renderTagsDialog = () => {
    const filteredTags = getFilteredTags();
    
    return (
      <Dialog
        header="Edit Your Tags"
        visible={showTagsDialog}
        style={{ width: '500px' }}
        onHide={() => setShowTagsDialog(false)}
        footer={
          <div className="flex justify-end gap-2 mt-4">
            <Button
              label="Cancel"
              icon="pi pi-times"
              onClick={() => setShowTagsDialog(false)}
              className="p-button-text"
            />
            <Button
              label="Save Tags"
              icon="pi pi-check"
              onClick={updateUserTags}
              loading={updating}
              disabled={updating}
            />
            </div>
        }
      >
        <div className="mb-4">
          <p className="text-sm text-gray-600 mb-3">
            Select tags that reflect your interests. These tags will help others understand your interests and will be used to recommend content.
          </p>
          
          {/* Tag search input */}
          <div className="relative mb-2">
            <input
              type="text"
              placeholder="Search tags..."
              value={tagSearchQuery}
              onChange={(e) => setTagSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-sm border rounded-md focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <FaTags className="absolute left-2.5 top-2 text-gray-400 text-sm" />
            {tagSearchQuery && (
              <button
                onClick={() => setTagSearchQuery('')}
                className="absolute right-2 top-1.5 text-gray-400 hover:text-gray-600"
              >
                ×
              </button>
            )}
          </div>
          
          {/* Selected tags section */}
          {selectedTags.length > 0 && (
            <div className="mb-2 border-b pb-2">
              <div className="text-xs text-gray-500 mb-1">Selected:</div>
              <div className="flex flex-wrap gap-1.5">
                {selectedTags.map(tagId => {
                  const tag = availableTags.find(t => t.id === tagId);
                  if (!tag) return null;
                  
                  return (
                    <Tag
                      key={tag.id}
                      tag={tag}
                      onRemove={() => handleTagSelection(tag.id)}
                    />
                  );
                })}
            </div>
            </div>
          )}
          
          {/* Tags list */}
          <div className="bg-gray-50 p-2 rounded max-h-48 overflow-y-auto border border-gray-100">
            {loadingTags ? (
              <div className="text-center py-4 text-gray-500 text-sm">Loading tags...</div>
            ) : filteredTags.length === 0 ? (
              <div className="text-center py-4 text-gray-500 text-sm">
                {availableTags.length === 0 ? "No tags available" : "No tags match your search"}
            </div>
            ) : (
              <div className="flex flex-wrap gap-1.5">
                {filteredTags
                  .filter(tag => !selectedTags.includes(tag.id)) // Don't show already selected tags
                  .map(tag => (
                    <div 
                      key={tag.id} 
                      onClick={() => handleTagSelection(tag.id)}
                      className="cursor-pointer"
                    >
                      <Tag tag={tag} />
                    </div>
                  ))
                }
              </div>
            )}
          </div>
        </div>
      </Dialog>
    );
  };

  // Apply sorting to threads
  const applySorting = (threads: Thread[]) => {
    // Create a new array to avoid mutating the original
    const sortedThreads = [...threads];
    
    switch(sortBy) {
      case 'newest':
        sortedThreads.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case 'oldest':
        sortedThreads.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        break;
      case 'mostVoted':
        sortedThreads.sort((a, b) => (b.upvoteCount - b.downvoteCount) - (a.upvoteCount - a.downvoteCount));
        break;
      case 'leastVoted':
        sortedThreads.sort((a, b) => (a.upvoteCount - a.downvoteCount) - (b.upvoteCount - b.downvoteCount));
        break;
      default:
        // Default to newest first
        sortedThreads.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }
    
    return sortedThreads;
  };

  // Build query parameters for the API call
  const buildQueryParams = () => {
    let queryParams = '';
    
    // Add sorting
    switch(sortBy) {
      case 'newest':
        queryParams += 'sort=createdAt,desc';
        break;
      case 'oldest':
        queryParams += 'sort=createdAt,asc';
        break;
      case 'mostVoted':
        queryParams += 'sort=upvoteCount,desc';
        break;
      case 'leastVoted':
        queryParams += 'sort=upvoteCount,asc';
        break;
      default:
        queryParams += 'sort=createdAt,desc';
    }
    
    return queryParams;
  };

  // Add a function to fetch user's threads
  const fetchUserThreads = async (userId: number) => {
    setLoadingThreads(true);
    setThreadsError(null);
    
    try {
      const queryParams = buildQueryParams();
      const response = await fetchWithAuth(`${API_ENDPOINTS.threads.byUser(userId)}?${queryParams}`);
      
      if (response.ok) {
        const result = await response.json();
        if (result && result.data) {
          // Store all threads for client-side filtering
          const threads = Array.isArray(result.data) ? result.data : 
                       (result.data.content ? result.data.content : []);
          
          setAllUserThreads(threads);
          
          // Apply client-side filtering and pagination
          applyFiltersAndPagination(threads);
        } else {
          setUserThreads([]);
          setAllUserThreads([]);
          setTotalPages(1);
        }
      } else {
        await handleAuthError(response, navigate);
        setThreadsError('Failed to fetch user threads');
      }
    } catch (error) {
      console.error('Error fetching user threads:', error);
      setThreadsError('An error occurred while loading threads');
    } finally {
      setLoadingThreads(false);
    }
  };
  
  // Apply filters and pagination to threads
  const applyFiltersAndPagination = (threads: Thread[]) => {
    // First apply sorting
    let filteredThreads = applySorting(threads);
    
    // Filter by tags
    if (threadFilterTags.length > 0) {
      filteredThreads = filteredThreads.filter(thread => 
        thread.tags.some(tag => threadFilterTags.includes(tag.id))
      );
    }
    
    // Filter by date
    if (dateFilter !== 'all') {
      const now = new Date();
      let cutoffDate = new Date();
      
      switch(dateFilter) {
        case 'today':
          cutoffDate.setDate(now.getDate() - 1);
          break;
        case 'week':
          cutoffDate.setDate(now.getDate() - 7);
          break;
        case 'month':
          cutoffDate.setMonth(now.getMonth() - 1);
          break;
        case 'year':
          cutoffDate.setFullYear(now.getFullYear() - 1);
          break;
      }
      
      filteredThreads = filteredThreads.filter(thread => 
        new Date(thread.createdAt) >= cutoffDate
      );
    }
    
    // Calculate total pages based on filtered threads
    const totalFilteredThreads = filteredThreads.length;
    setTotalPages(Math.ceil(totalFilteredThreads / pageSize) || 1);
    
    // Apply pagination
    const startIndex = (currentPage - 1) * pageSize;
    const paginatedThreads = filteredThreads.slice(startIndex, startIndex + pageSize);
    
    setUserThreads(paginatedThreads);
  };
  
  // Add a useEffect to fetch threads after user data is loaded
  useEffect(() => {
    if (user) {
      fetchUserThreads(user.id);
    }
  }, [user]);
  
  // Add a useEffect to reapply filters when they change
  useEffect(() => {
    if (allUserThreads.length > 0) {
      applyFiltersAndPagination(allUserThreads);
    }
  }, [currentPage, threadFilterTags, dateFilter, sortBy]);

  // Handle page change
  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    setSearchParams({ page: newPage.toString() });
    window.scrollTo(0, 0);
  };
  
  // Handle thread filter tag selection
  const handleThreadTagSelection = (tagId: number) => {
    if (threadFilterTags.includes(tagId)) {
      setThreadFilterTags(threadFilterTags.filter(id => id !== tagId));
    } else {
      setThreadFilterTags([...threadFilterTags, tagId]);
    }
    // Reset to page 1 when changing filters
    setSearchParams({ page: '1' });
  };
  
  // Reset all thread filters
  const resetThreadFilters = () => {
    setThreadFilterTags([]);
    setDateFilter('all');
    setSortBy('newest');
    setSearchParams({ page: '1' });
  };

  // Generate pagination controls
  const renderPagination = () => {
    if (totalPages <= 1) return null;
    
    return (
      <div className="flex justify-center mt-8">
        <nav className="flex items-center space-x-2">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className={`p-2 rounded ${
              currentPage === 1 
                ? 'text-gray-400 cursor-not-allowed' 
                : 'text-blue-600 hover:bg-blue-50'
            }`}
            aria-label="Previous page"
          >
            <FaChevronLeft />
          </button>
          
          {/* Show page numbers with ellipsis for large page counts */}
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            // For small page counts, just show sequential pages
            let pageNum = i + 1;
            
            // For larger page counts, create a window around the current page
            if (totalPages > 5) {
              if (currentPage <= 3) {
                // Near the start
                pageNum = i + 1;
              } else if (currentPage >= totalPages - 2) {
                // Near the end
                pageNum = totalPages - (4 - i);
              } else {
                // In the middle
                pageNum = currentPage - 2 + i;
              }
            }
            
            return (
              <button
                key={pageNum}
                onClick={() => handlePageChange(pageNum)}
                className={`w-8 h-8 rounded flex items-center justify-center ${
                  currentPage === pageNum 
                    ? 'bg-blue-100 text-blue-700 font-medium' 
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                {pageNum}
              </button>
            );
          })}
          
          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className={`p-2 rounded ${
              currentPage === totalPages 
                ? 'text-gray-400 cursor-not-allowed' 
                : 'text-blue-600 hover:bg-blue-50'
            }`}
            aria-label="Next page"
          >
            <FaChevronRight />
          </button>
        </nav>
            </div>
    );
  };

  // Render filter sidebar for threads
  const renderThreadFilterSidebar = () => {
    return (
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-medium text-gray-800">Filter Threads</h3>
          <button 
            onClick={resetThreadFilters}
            className="text-xs text-blue-600 hover:text-blue-800"
          >
            Reset Filters
          </button>
            </div>
            
        <div className="mb-6">
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaSort className="mr-2 text-gray-500" />
            Sort By
          </h3>
          <div className="space-y-2">
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="newest" 
                checked={sortBy === 'newest'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Newest First</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="oldest" 
                checked={sortBy === 'oldest'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Oldest First</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="mostVoted" 
                checked={sortBy === 'mostVoted'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Most Voted</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="sortBy" 
                value="leastVoted" 
                checked={sortBy === 'leastVoted'}
                onChange={(e) => {
                  setSortBy(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Least Voted</span>
            </label>
          </div>
            </div>
            
        <div className="mb-6">
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaCalendarAlt className="mr-2 text-gray-500" />
            Date
          </h3>
          <div className="space-y-2">
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="all" 
                checked={dateFilter === 'all'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">All Time</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="today" 
                checked={dateFilter === 'today'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">Today</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="week" 
                checked={dateFilter === 'week'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">This Week</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="month" 
                checked={dateFilter === 'month'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">This Month</span>
            </label>
            <label className="flex items-center">
              <input 
                type="radio" 
                name="dateFilter" 
                value="year" 
                checked={dateFilter === 'year'}
                onChange={(e) => {
                  setDateFilter(e.target.value);
                  setSearchParams({ page: '1' });
                }}
                className="mr-2"
              />
              <span className="text-sm text-gray-700">This Year</span>
            </label>
          </div>
            </div>
            
        <div>
          <h3 className="flex items-center text-sm font-medium text-gray-700 mb-2">
            <FaTags className="mr-2 text-gray-500" />
            Filter by Tags
          </h3>
          <div className="mb-2">
            <InputText
              value={tagSearchQuery}
              onChange={(e) => setTagSearchQuery(e.target.value)}
              placeholder="Search tags..."
              className="w-full p-2 text-sm"
            />
              </div>
          
          {threadFilterTags.length > 0 && (
            <div className="mb-3 border-b pb-2">
              <div className="text-xs text-gray-500 mb-1">Selected Filters:</div>
              <div className="flex flex-wrap gap-1.5">
                {threadFilterTags.map(tagId => {
                  const tag = availableTags.find(t => t.id === tagId);
                  if (!tag) return null;
                  
                  return (
                    <div key={tag.id} className="flex items-center">
                      <Tag
                        tag={tag}
                        onRemove={() => handleThreadTagSelection(tag.id)}
                      />
            </div>
                  );
                })}
              </div>
            </div>
          )}
            
          <div className="max-h-40 overflow-y-auto mt-2">
            <div className="flex flex-wrap gap-1.5">
              {loadingTags ? (
                <div className="text-center text-gray-500 py-2 text-sm w-full">Loading tags...</div>
              ) : getFilteredTags().filter(tag => !threadFilterTags.includes(tag.id)).length === 0 ? (
                <div className="text-center text-gray-500 py-2 text-sm w-full">
                  {getFilteredTags().length === 0 ? "No tags found" : "All matching tags selected"}
              </div>
              ) : (
                getFilteredTags()
                  .filter(tag => !threadFilterTags.includes(tag.id))
                  .map(tag => (
                    <div 
                      key={tag.id}
                      onClick={() => handleThreadTagSelection(tag.id)}
                      className="cursor-pointer hover:opacity-80 transition-opacity"
                    >
                      <Tag tag={tag} />
            </div>
                  ))
              )}
            </div>
          </div>
        </div>
      </div>
    );
  };

  // In the render section for the threads content
  const renderThreadsContent = () => {
    if (loadingThreads && currentPage === 1) {
      return (
        <div className="flex flex-col items-center justify-center py-10">
          <i className="pi pi-spin pi-spinner text-blue-500 text-3xl mb-4"></i>
          <p className="text-gray-500">Loading threads...</p>
        </div>
      );
    }
    
    if (threadsError) {
      return (
        <div className="text-center py-8 text-red-500">
          <i className="pi pi-exclamation-triangle text-2xl mb-2 block"></i>
          <p>{threadsError}</p>
          <Button 
            label="Try Again" 
            icon="pi pi-refresh" 
            className="p-button-outlined p-button-danger mt-3"
            onClick={() => user && fetchUserThreads(user.id)} 
          />
        </div>
      );
    }
    
    if (userThreads.length === 0) {
      if (threadFilterTags.length > 0 || dateFilter !== 'all') {
        return (
          <div className="text-center py-10 px-4">
            <div className="bg-gray-50 rounded-lg p-6 inline-block mb-3">
              <i className="pi pi-search text-4xl text-gray-400"></i>
            </div>
            <h3 className="text-xl font-medium text-gray-700 mb-2">No threads match your filters</h3>
            <p className="text-gray-500 mb-4 max-w-md mx-auto">
              Try adjusting your filters or <button onClick={resetThreadFilters} className="text-blue-600 hover:underline">reset all filters</button>.
            </p>
          </div>
        );
      }
      
      return (
        <div className="text-center py-10 px-4">
          <div className="bg-gray-50 rounded-lg p-6 inline-block mb-3">
            <i className="pi pi-inbox text-4xl text-gray-400"></i>
          </div>
          <h3 className="text-xl font-medium text-gray-700 mb-2">No threads yet</h3>
          <p className="text-gray-500 mb-4 max-w-md mx-auto">
            {isOwnProfile 
              ? "You haven't created any threads yet."
              : user ? `${user.firstName} hasn't created any threads yet.` : "This user hasn't created any threads yet."}
          </p>
        </div>
      );
    }
    
    return (
      <div>
        <div className="mb-4 flex items-center justify-between">
          <div className="text-sm text-gray-500">
            Showing {userThreads.length} of {allUserThreads.length} threads (page {currentPage} of {totalPages})
          </div>
          <div className="lg:hidden">
            <Button
              icon={<FaFilter className="mr-2" />}
              label="Filters"
              className="p-button-outlined p-button-sm"
              onClick={() => setShowFilters(!showFilters)}
            />
          </div>
        </div>
        
        {/* Mobile filter panel */}
        {showFilters && (
          <div className="lg:hidden mb-6">
            {renderThreadFilterSidebar()}
          </div>
        )}
        
        <div className="space-y-4">
          {userThreads.map(thread => (
            <ThreadCard key={thread.id} thread={thread} />
          ))}
        </div>
        
        {renderPagination()}
      </div>
    );
  };

  const renderProfile = (user: User) => (
    <div className="flex flex-col lg:flex-row gap-6">
      {/* Profile Card - Left Side */}
      <div className="bg-white rounded-xl shadow p-6 w-full lg:max-w-xl">
        <Toast ref={toast} position="top-right" />
        <div className="flex flex-col gap-5">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-gray-100">
            <div className="text-2xl font-bold text-blue-800">Profile</div>
            {!isOwnProfile && (
              <Button
                label={isFollowing ? 'Unfollow' : 'Follow'}
                icon={isFollowing ? <FaUserMinus className="mr-2" /> : <FaUserPlus className="mr-2" />}
                onClick={handleFollow}
                className={isFollowing ? 'p-button-outlined p-button-danger' : 'p-button-outlined p-button-primary'}
                size="small"
              />
            )}
          </div>
          
          {/* User info header */}
          <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 mb-6 pb-6 border-b border-gray-100">
            <div className="w-24 h-24 flex items-center justify-center bg-blue-50 rounded-full">
              <Avatar label={user.initials} shape="circle" size="xlarge" style={{ backgroundColor: '#4a7bff', color: 'white', width: '5rem', height: '5rem', fontSize: '2rem' }} />
            </div>
            
            <div className="flex-1 text-center sm:text-left">
              <span className="text-xl font-bold text-gray-800 block mb-1">
                {user.firstName} {user.lastName}
              </span>
              <span className="text-gray-500">@{user.username}</span>
              
              {/* Stats cards */}
              <div className="flex flex-wrap justify-center sm:justify-start gap-3 mt-3">
                <div className="bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg px-4 py-2 flex items-center shadow-sm">
                  <i className="pi pi-users text-blue-500 mr-2 text-lg"></i>
                  <div>
                    <div className="font-bold text-gray-900">{followerCount}</div>
                    <div className="text-xs text-gray-500">Followers</div>
                  </div>
                </div>
                <div className="bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg px-4 py-2 flex items-center shadow-sm">
                  <i className="pi pi-user-plus text-blue-500 mr-2 text-lg"></i>
                  <div>
                    <div className="font-bold text-gray-900">{followingCount}</div>
                    <div className="text-xs text-gray-500">Following</div>
                  </div>
                </div>
            {user.createdAt && (
                  <div className="bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg px-4 py-2 flex items-center shadow-sm">
                    <i className="pi pi-calendar text-blue-500 mr-2 text-lg"></i>
                    <div>
                      <div className="text-xs text-gray-500">Joined</div>
                      <div className="text-sm text-gray-900">{formatDate(user.createdAt)}</div>
                    </div>
              </div>
            )}
          </div>
        </div>
      </div>
          
          {/* Profile details */}
          <div className="flex flex-col text-sm space-y-3">
            {/* Email section with special styling since it's not editable */}
            <div className="bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg p-4 shadow-sm">
              <div className="flex items-center gap-3">
                <i className="pi pi-envelope text-blue-500 text-lg"></i>
                <div className="flex-1">
                  <div className="text-xs text-gray-500 mb-1">Email</div>
                  <div className="font-medium text-gray-800">{user.email}</div>
                </div>
              </div>
            </div>
            
            {renderEditableFieldCard('Birth Date', 'birthDate', user.birthDate || '')}
            {renderEditableFieldCard('Bio', 'bio', user.bio || '')}
            {renderEditableFieldCard('Location', 'location', user.location || '')}
            {renderEditableFieldCard('Profession', 'profession', user.profession || '')}
            
            {/* Tags section */}
            <div className="bg-gray-50 hover:bg-gray-100 transition-colors rounded-lg p-4 shadow-sm">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <i className="pi pi-tags text-blue-500 text-lg"></i>
                  <div className="text-md font-medium text-gray-700">Tags</div>
                </div>
                {isOwnProfile && (
                  <Button 
                    icon="pi pi-pencil" 
                    rounded
                    outlined
                    size="small"
                    onClick={() => setShowTagsDialog(true)}
                    className="p-button-sm"
                  />
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {user.tags && user.tags.length > 0 ? (
                  user.tags.map(tag => (
                    <Tag key={tag.id} tag={tag} />
                  ))
                ) : (
                  <span className="text-gray-500 text-sm italic">No tags added</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* Right side - User threads */}
      <div className="flex-1 mt-6 lg:mt-0">
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Thread list */}
          <div className="flex-1">
            <div className="bg-white rounded-xl shadow p-6">
              <div className="flex items-center mb-4 pb-2 border-b border-gray-100">
                <div className="flex items-center gap-2">
                  <i className="pi pi-list text-blue-500 text-xl"></i>
                  <h2 className="text-xl font-bold text-blue-800">Created Threads</h2>
                  {allUserThreads.length > 0 && (
                    <span className="bg-blue-100 text-blue-800 text-xs font-medium rounded-full px-2.5 py-0.5 ml-1">
                      {allUserThreads.length}
                    </span>
                  )}
                </div>
              </div>
              
              {renderThreadsContent()}
            </div>
          </div>
          
          {/* Filter sidebar - desktop only */}
          <div className="hidden lg:block lg:w-64 sticky top-24 self-start">
            {renderThreadFilterSidebar()}
          </div>
        </div>
      </div>
      
      {/* Tags Dialog */}
      {renderTagsDialog()}
    </div>
  );

  return (
    <MainLayout>
      {() => (
        loading ? (
          <div className="flex justify-center items-center py-6">
            <p className="text-gray-600">Loading profile...</p>
          </div>
        ) : user ? (
          renderProfile(user)
        ) : (
          <div className="bg-white rounded-xl shadow-sm p-6 max-w-md">
            <div className="text-center py-4 text-gray-500">
              {error || 'User not found'}
            </div>
          </div>
        )
      )}
    </MainLayout>
  );
};

export default UserProfile; 