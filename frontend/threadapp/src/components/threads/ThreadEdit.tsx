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
}

const ThreadEdit = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  
  const [thread, setThread] = useState<Thread | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedTags, setSelectedTags] = useState<Tag[]>([]);
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
  
  const handleTagsChange = (newTags: Tag[]) => {
    console.log('Tags changed to:', newTags);
    setSelectedTags(newTags);
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!title.trim() || !thread) {
      toast.current?.show({ severity: 'error', summary: 'Validation Error', detail: 'Title is required or thread data is missing' });
      return;
    }
    
    try {
      setSaving(true);
      setError(null);
      
      const response = await fetchWithAuth(`${API_ENDPOINTS.threads.update(Number(id))}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          id: Number(id),
          title,
          description,
          authorId: thread.authorId,
          tags: selectedTags
        })
      });
      
      if (!response.ok) {
        if (handleAuthError(response, navigate)) return;
        
        try {
          const errorData = await response.json();
          console.error('Error updating thread:', errorData);
          setError(errorData.message || 'Failed to update thread');
          toast.current?.show({ 
            severity: 'error', 
            summary: 'Error', 
            detail: errorData.message || 'Failed to update thread' 
          });
        } catch (parseError) {
          console.error('Error parsing error response:', parseError);
          console.error('Original response:', response);
          setError('Failed to update thread. Server error.');
          toast.current?.show({ 
            severity: 'error', 
            summary: 'Error', 
            detail: 'Failed to update thread. Server error.' 
          });
        }
        return;
      }
      
      toast.current?.show({ severity: 'success', summary: 'Success', detail: 'Thread updated successfully' });
      
      // Redirect to thread view after a short delay
      setTimeout(() => {
        navigate(`/threads/${id}`);
      }, 1500);
    } catch (error) {
      console.error('Error updating thread:', error);
      setError(error instanceof Error ? error.message : 'An error occurred while updating the thread');
      toast.current?.show({ severity: 'error', summary: 'Error', detail: 'An error occurred while updating the thread' });
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
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Edit Thread</h1>
          <div className="flex" style={{ gap: "24px" }}>
            <div>
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
            </div>
            
            {isUserAdmin && !thread.active && (
              <div>
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
              </div>
            )}
          </div>
        </div>
        
        {!thread.active && (
          <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-md flex items-center">
            <FaExclamationTriangle className="text-yellow-500 mr-2" />
            <span className="text-sm text-yellow-700">
              This thread is currently deactivated and not visible to other users.
              {!isUserAdmin && thread.deactivatedByRole === 'ADMIN' && 
                ' It was deactivated by an admin and only an admin can reactivate it.'}
              {isUserAdmin && ' As an admin, you can permanently delete this thread now.'}
            </span>
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
          
          <div className="flex justify-between items-center mt-6">
            <Button 
              type="button" 
              label="Cancel" 
              severity="secondary"
              onClick={() => navigate(`/threads/${id}`)} 
              disabled={saving}
            />
            
            <Button 
              type="submit" 
              label={saving ? 'Saving...' : 'Save Changes'} 
              icon="pi pi-save" 
              disabled={saving}
            />
          </div>
        </form>
      </div>
      
      <Toast ref={toast} position="bottom-right" />
      <ConfirmDialog />
    </MainLayout>
  );
};

export default ThreadEdit; 