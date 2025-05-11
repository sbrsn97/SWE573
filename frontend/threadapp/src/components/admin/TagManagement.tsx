import React, { useState, useEffect, useCallback, useRef } from 'react';
import { fetchWithAuth } from '../../utils/authUtils';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import { FaTrash, FaPlus, FaSearch } from 'react-icons/fa';
import { Tag as TagType } from '../tags/TagSelector';
import TagComponent from '../tags/Tag';
import CreateTagModal from '../tags/CreateTagModal';
import { Toast } from 'primereact/toast';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { useNavigate } from 'react-router-dom';
import { handleAuthError } from '../../utils/authUtils';

const TagManagement: React.FC = () => {
  const [tags, setTags] = useState<TagType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const toast = useRef<Toast>(null);
  const navigate = useNavigate();

  // Fetch all tags
  const fetchTags = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.tags.getAll);
      
      // Handle authentication errors
      if (response.status === 401) {
        if (handleAuthError(response, navigate)) {
          return;
        }
      }
      
      if (!response.ok) {
        throw new Error(`Failed to fetch tags: ${response.status}`);
      }
      
      const data = await response.json();
      if (data.success) {
        setTags(data.data);
      } else {
        throw new Error(data.message || 'Failed to fetch tags');
      }
    } catch (err) {
      setError('Failed to load tags. Please try again.');
      console.error('Error fetching tags:', err);
      toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load tags' });
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  // Filter tags based on search term
  const filteredTags = tags.filter(tag => 
    tag.label.toLowerCase().includes(searchTerm.toLowerCase()) ||
    tag.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
    tag.wikidataEntityId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Handle tag creation
  const handleTagCreated = (newTag: TagType) => {
    setTags(prevTags => [...prevTags, newTag]);
    setShowCreateModal(false);
    toast.current?.show({ severity: 'success', summary: 'Success', detail: `Tag "${newTag.label}" created successfully` });
  };

  // Handle tag deletion
  const handleDeleteTag = async (tag: TagType) => {
    const tagId = typeof tag.id === 'string' ? parseInt(tag.id, 10) : tag.id;
    
    // Use a simple browser confirm
    const confirmed = window.confirm(`Are you sure you want to delete the tag "${tag.label}"? This action cannot be undone and may affect threads using this tag.`);
    
    if (confirmed) {
      setLoading(true);
      
      try {
        const deleteUrl = API_ENDPOINTS.tags.delete(tagId);
        
        // Get token directly
        const token = localStorage.getItem('token');
        if (!token) {
          throw new Error('Authentication token not found');
        }
        
        // Use fetch directly with promises instead of async/await
        fetch(deleteUrl, {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        })
        .then(response => {
          if (response.status === 401) {
            handleAuthError(response, navigate);
            return;
          }
          
          if (!response.ok) {
            return response.json().catch(() => {
              throw new Error(`Failed to delete tag: ${response.status} ${response.statusText}`);
            }).then(errorData => {
              throw new Error(errorData.message || `Failed to delete tag: ${response.status}`);
            });
          }
          
          // Success path
          setTags(prevTags => prevTags.filter(t => t.id !== tag.id));
          toast.current?.show({ 
            severity: 'success', 
            summary: 'Success', 
            detail: `Tag "${tag.label}" deleted successfully`
          });
          
          // Refetch the tags
          fetchTags();
        })
        .catch(err => {
          console.error('Error deleting tag:', err);
          toast.current?.show({ 
            severity: 'error', 
            summary: 'Error', 
            detail: err.message || 'Failed to delete tag',
            life: 5000
          });
        })
        .finally(() => {
          setLoading(false);
        });
      } catch (err) {
        console.error('Error in delete setup:', err);
        toast.current?.show({ 
          severity: 'error', 
          summary: 'Error', 
          detail: err instanceof Error ? err.message : 'Failed to set up tag deletion',
          life: 5000
        });
        setLoading(false);
      }
    }
  };

  return (
    <MainLayout>
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-6">Tag Management</h1>
        <p className="text-gray-600 mb-8">
          Manage tags across the platform. You can create new tags, search existing tags, and delete unused tags.
        </p>

        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
            <div className="relative w-full md:w-1/2">
              <div className="relative flex items-center">
                <FaSearch className="absolute left-3 text-gray-400 pointer-events-none" />
                <InputText
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search tags..."
                  className="w-full pl-10 pr-3 py-2"
                  style={{ paddingLeft: '2.5rem' }}
                />
              </div>
            </div>
            
            <Button 
              icon={<FaPlus className="mr-2" />}
              label="Create New Tag" 
              onClick={() => setShowCreateModal(true)}
              className="w-full md:w-auto"
            />
          </div>

          {loading && tags.length === 0 ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
            </div>
          ) : error ? (
            <div className="bg-red-50 p-4 rounded-md text-red-600">
              {error}
            </div>
          ) : filteredTags.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              {searchTerm ? 'No tags found matching your search.' : 'No tags have been created yet.'}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tag</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Wikidata ID</th>
                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredTags.map((tag) => (
                    <tr key={tag.id}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <TagComponent tag={tag} />
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-gray-900 truncate max-w-[200px]" title={tag.description}>
                          {tag.description || 'No description'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {tag.wikidataEntityId || 'N/A'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <button
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            handleDeleteTag(tag);
                          }}
                          className="flex items-center justify-center p-2 rounded-full text-red-600 hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-500"
                          aria-label="Delete tag"
                        >
                          <FaTrash />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {showCreateModal && (
        <CreateTagModal
          onClose={() => setShowCreateModal(false)}
          onTagCreated={handleTagCreated}
        />
      )}
      
      <Toast ref={toast} position="bottom-right" />
    </MainLayout>
  );
};

export default TagManagement; 