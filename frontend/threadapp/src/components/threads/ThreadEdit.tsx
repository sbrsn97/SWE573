import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { Toast } from 'primereact/toast';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog';
import { Tag as PTag } from 'primereact/tag';
import MainLayout from '../layout/MainLayout';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth, handleAuthError, isAdmin, isThreadOwner } from '../../utils/authUtils';
import { FaEdit, FaTrash, FaArchive, FaUndo, FaExclamationTriangle } from 'react-icons/fa';
import TagSelector from '../tags/TagSelector';
import { Tag } from '../tags/TagSelector';
import { isProfanityError, formatProfanityError, ProfanityErrorMessage } from '../../utils/errorUtils';
import { ThreadStyle } from './CreateThreadPage';
import { Dropdown } from 'primereact/dropdown';

// Thread visibility descriptions
const threadStyleDescriptions = {
  [ThreadStyle.PUBLIC]: 'Anyone can view and interact with this thread',
  [ThreadStyle.PRIVATE]: 'Only you and explicitly invited users can view and interact with this thread',
  [ThreadStyle.FOLLOW_TO_INTERACT]: 'Anyone can view, but only followers can comment or vote'
};

interface Thread {
  id: number;
  title: string;
  description: string;
  authorId: number;
  createdAt: string;
  updatedAt: string;
  active: boolean;
  upvoteCount: number;
  downvoteCount: number;
  deactivatedByRole: string | null;
  author: {
    id: number;
    username: string;
  };
  tags: Tag[];
  threadStyle?: ThreadStyle;
}

const ThreadEdit = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  
  const [thread, setThread] = useState<Thread | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTags, setSelectedTags] = useState<Tag[]>([]);
  const [threadStyle, setThreadStyle] = useState<ThreadStyle>(ThreadStyle.PUBLIC);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isUserAdmin, setIsUserAdmin] = useState(false);
  const [isOwner, setIsOwner] = useState(false);
  const [confirmDialogVisible, setConfirmDialogVisible] = useState(false);
  const [actionType, setActionType] = useState<'deactivate' | 'reactivate'>('deactivate');
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchThread = async () => {
      if (!id) return;
      
      try {
        setLoading(true);
        const response = await fetchWithAuth(`${API_ENDPOINTS.threads.get(Number(id))}`);
        
        if (!response.ok) {
          if (handleAuthError(response, navigate)) return;
          
          if (response.status === 404) {
            toast.current?.show({ severity: 'error', summary: 'Not Found', detail: 'Thread not found' });
            navigate('/home');
            return;
          }
          
          toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to load thread' });
          return;
        }
        
        const { data } = await response.json();
        setThread(data);
        setTitle(data.title);
        setDescription(data.description);
        setSelectedTags(data.tags || []);
        setThreadStyle(data.threadStyle || ThreadStyle.PUBLIC);
        
        // Check permissions
        const [admin, owner] = await Promise.all([
          isAdmin(),
          isThreadOwner(Number(id))
        ]);
        
        setIsUserAdmin(admin);
        setIsOwner(owner);
        
        // If user has no permission to edit, redirect
        if (!admin && !owner) {
          toast.current?.show({ severity: 'error', summary: 'Unauthorized', detail: 'You do not have permission to edit this thread' });
          navigate(`/threads/${id}`);
        }
      } catch (error) {
        console.error('Error fetching thread:', error);
        toast.current?.show({ severity: 'error', summary: 'Error', detail: 'An error occurred while loading the thread' });
      } finally {
        setLoading(false);
      }
    };
    
    fetchThread();
  }, [id, navigate]);
  
  useEffect(() => {
    if (thread) {
      setTitle(thread.title);
      setDescription(thread.description || '');
      setSelectedTags(thread.tags || []);
      setThreadStyle(thread.threadStyle || ThreadStyle.PUBLIC);
    }
  }, [thread]);
  
  const handleTagsChange = (newTags: Tag[]) => {
    console.log('Tags changed to:', newTags);
    setSelectedTags(newTags);
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    
    try {
      const response = await fetchWithAuth(`${API_ENDPOINTS.threads.update(Number(id))}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title,
          description,
          tags: selectedTags,
          threadStyle: threadStyle,
          authorId: thread?.authorId
        })
      });
      
      if (response.status === 401) {
        if (handleAuthError(response, navigate)) {
          return;
        }
      }
      
      if (!response.ok) {
        if (isProfanityError(await response.clone().json())) {
          setError(formatProfanityError(await response.json()));
        } else {
          const errorData = await response.json();
          setError(errorData.message || 'Failed to update thread');
        }
        setSaving(false);
        return;
      }
      
      const result = await response.json();
      setThread(result.data);
      
      toast.current?.show({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Thread updated successfully' 
      });
      
      // Navigate back to thread view
      navigate(`/threads/${id}`);
    } catch (err) {
      console.error('Error updating thread:', err);
      setError('An error occurred while updating the thread');
    } finally {
      setSaving(false);
    }
  };
  
  const confirmAction = (type: 'deactivate' | 'reactivate') => {
    setActionType(type);
    
    let message = '';
    let header = '';
    
    switch (type) {
      case 'deactivate':
        header = 'Deactivate Thread';
        message = 'Are you sure you want to deactivate this thread? It will be hidden from other users but can be reactivated later.';
        break;
      case 'reactivate':
        header = 'Reactivate Thread';
        message = 'Are you sure you want to reactivate this thread? It will be visible to all users again.';
        break;
    }
    
    confirmDialog({
      message,
      header,
      icon: 'pi pi-info-circle',
      acceptClassName: 'p-button-primary',
      accept: () => {
        handleActionConfirmed();
      }
    });
  };
  
  const handleActionConfirmed = async () => {
    if (!thread) {
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Thread data is missing. Please try reloading the page.' 
      });
      return;
    }
    
    try {
      setSaving(true);
      let response;
      
      switch (actionType) {
        case 'deactivate':
          response = await fetchWithAuth(`${API_ENDPOINTS.threads.delete(thread.id)}`, {
            method: 'DELETE'
          });
          break;
        case 'reactivate':
          response = await fetchWithAuth(`${API_ENDPOINTS.threads.reactivate(thread.id)}`, {
            method: 'POST'
          });
          break;
      }
      
      if (!response || !response.ok) {
        if (response && handleAuthError(response, navigate)) return;
        
        toast.current?.show({ 
          severity: 'error', 
          summary: 'Error', 
          detail: `Failed to ${actionType} thread` 
        });
        return;
      }
      
      toast.current?.show({ 
        severity: 'success', 
        summary: 'Success', 
        detail: actionType === 'reactivate' ? 'Thread reactivated' : 'Thread deactivated'
      });
      
      // Refresh the thread data
      const refreshResponse = await fetchWithAuth(`${API_ENDPOINTS.threads.get(Number(id))}`);
      if (refreshResponse.ok) {
        const { data } = await refreshResponse.json();
        setThread(data);
      }
    } catch (error) {
      console.error(`Error during ${actionType}:`, error);
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: `An error occurred while attempting to ${actionType} the thread` 
      });
    } finally {
      setSaving(false);
    }
  };
  
  // Add a dedicated function for thread reactivation
  const handleThreadReactivate = async () => {
    if (!thread) {
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Thread data is missing. Please try reloading the page.' 
      });
      return;
    }
    
    try {
      setSaving(true);
      
      const response = await fetch(`${API_ENDPOINTS.threads.reactivate(thread.id)}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        
        toast.current?.show({ 
          severity: 'error', 
          summary: 'Error', 
          detail: 'Failed to reactivate thread' 
        });
        return;
      }
      
      toast.current?.show({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Thread reactivated successfully'
      });
      
      // Refresh the thread data
      const refreshResponse = await fetchWithAuth(`${API_ENDPOINTS.threads.get(Number(id))}`);
      if (refreshResponse.ok) {
        const { data } = await refreshResponse.json();
        setThread(data);
      }
    } catch (error) {
      console.error('Error during reactivation:', error);
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'An error occurred while attempting to reactivate the thread' 
      });
    } finally {
      setSaving(false);
    }
  };
  
  // Add a dedicated function for hard deletion
  const handleThreadHardDelete = async () => {
    if (!thread) {
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Thread data is missing. Please try reloading the page.' 
      });
      return;
    }
    
    try {
      setSaving(true);
      
      // Call the hard delete endpoint directly
      const response = await fetch(`${API_ENDPOINTS.threads.hardDelete(thread.id)}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        
        // Try to get error message from response
        let errorMessage = 'Failed to permanently delete thread';
        try {
          const errorData = await response.json();
          if (errorData.message) {
            errorMessage = errorData.message;
          }
        } catch (e) {
          // If we can't parse the error, use generic message
        }
        
        toast.current?.show({ 
          severity: 'error', 
          summary: 'Error', 
          detail: errorMessage
        });
        return;
      }
      
      toast.current?.show({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Thread permanently deleted'
      });
      
      // Redirect to home after successful hard delete
      setTimeout(() => {
        navigate('/home');
      }, 1500);
      
    } catch (error) {
      console.error('Error during hard deletion:', error);
      toast.current?.show({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'An error occurred while attempting to permanently delete the thread' 
      });
    } finally {
      setSaving(false);
    }
  };
  
  if (loading) {
    return (
      <MainLayout>
        <div className="flex justify-center items-center h-screen">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-500"></div>
        </div>
      </MainLayout>
    );
  }
  
  if (!thread) {
    return (
      <MainLayout>
        <div className="text-center mt-8">
          <h2 className="text-2xl font-bold text-gray-800">Thread not found</h2>
          <Button label="Go Back" icon="pi pi-arrow-left" onClick={() => navigate('/home')} className="mt-4" />
        </div>
      </MainLayout>
    );
  }
  
  return (
    <MainLayout>
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <div className="mb-6 flex items-center justify-between">
            <h1 className="text-2xl font-bold">Edit Thread</h1>
            <div className="flex gap-2">
              {thread.active ? (
                <Button 
                  icon={<FaArchive className="mr-2" />}
                  label="Deactivate" 
                  severity="secondary"
                  onClick={() => confirmAction('deactivate')} 
                  disabled={saving}
                />
              ) : (
                <Button 
                  icon={<FaUndo className="mr-2" />}
                  label="Reactivate" 
                  severity="info"
                  onClick={() => {
                    // Show confirmation dialog
                    confirmDialog({
                      message: 'Are you sure you want to reactivate this thread? It will be visible to all users again.',
                      header: 'Reactivate Thread',
                      icon: 'pi pi-info-circle',
                      acceptClassName: 'p-button-primary',
                      accept: () => handleThreadReactivate()
                    });
                  }} 
                  disabled={saving || (!isUserAdmin && thread.deactivatedByRole === 'ADMIN')}
                />
              )}
              
              {isUserAdmin && !thread.active && (
                <Button 
                  icon={<FaTrash className="mr-2" />}
                  label="Hard Delete" 
                  severity="danger"
                  onClick={() => {
                    // Show confirmation dialog for hard delete
                    confirmDialog({
                      message: 'Are you sure you want to permanently delete this thread? This action CANNOT be undone!',
                      header: 'Permanently Delete Thread',
                      icon: 'pi pi-exclamation-triangle',
                      acceptClassName: 'p-button-danger',
                      accept: () => handleThreadHardDelete()
                    });
                  }} 
                  disabled={saving}
                />
              )}
            </div>
          </div>
          
          {!thread.active && (
            <div className="mb-6 p-4 bg-yellow-50 border-l-4 border-yellow-400 text-yellow-800">
              <div className="flex">
                <div className="flex-shrink-0">
                  <FaExclamationTriangle className="h-5 w-5 text-yellow-400" />
                </div>
                <div className="ml-3">
                  <p className="text-sm">
                    This thread is currently inactive and not visible to users.
                  </p>
                </div>
              </div>
            </div>
          )}
          
          <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow-md">
            {error && <ProfanityErrorMessage message={error} />}
            
            <div className="mb-4">
              <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
                Title *
              </label>
              <InputText
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full"
                required
              />
            </div>
            
            <div className="mb-4">
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <InputTextarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={5}
                className="w-full"
              />
            </div>
            
            <div className="mb-4">
              <label htmlFor="threadStyle" className="block text-sm font-medium text-gray-700 mb-1">
                Thread Visibility
              </label>
              <Dropdown
                id="threadStyle"
                value={threadStyle}
                onChange={(e) => setThreadStyle(e.value)}
                options={Object.values(ThreadStyle).map(style => ({
                  label: style.replace(/_/g, ' '),
                  value: style
                }))}
                placeholder="Select Thread Visibility"
                className="w-full"
              />
              <p className="mt-1 text-sm text-gray-500">
                {threadStyle ? threadStyleDescriptions[threadStyle] : ''}
              </p>
            </div>
            
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Tags
              </label>
              <div className="text-xs text-gray-500 mb-2">
                {selectedTags.length > 0 
                  ? `${selectedTags.length} tag${selectedTags.length > 1 ? 's' : ''} selected` 
                  : 'No tags selected'}
              </div>
              <TagSelector
                selectedTags={selectedTags}
                onTagsChange={handleTagsChange}
              />
            </div>
            
            <div className="flex justify-end gap-4">
              <Button 
                label="Cancel" 
                className="p-button-secondary" 
                onClick={() => navigate(`/threads/${id}`)}
                disabled={saving}
              />
              <Button 
                label={saving ? "Saving..." : "Save Changes"} 
                type="submit" 
                disabled={saving}
              />
            </div>
          </form>
          
          <Toast ref={toast} />
          <ConfirmDialog />
        </div>
      </div>
    </MainLayout>
  );
};

export default ThreadEdit; 