import React, { useState, useRef } from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Toast } from 'primereact/toast';
import { Password } from 'primereact/password';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth, handleAuthError } from '../../utils/authUtils';

interface ChangePasswordDialogProps {
  userId: number;
  visible: boolean;
  onHide: () => void;
  onSuccess?: () => void;
}

const ChangePasswordDialog: React.FC<ChangePasswordDialogProps> = ({
  userId,
  visible,
  onHide,
  onSuccess
}) => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const toast = useRef<Toast>(null);

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    
    if (!currentPassword) {
      newErrors.currentPassword = 'Current password is required';
    }
    
    if (!newPassword) {
      newErrors.newPassword = 'New password is required';
    } else if (newPassword.length < 8) {
      newErrors.newPassword = 'New password must be at least 8 characters long';
    }
    
    if (!confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your new password';
    } else if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }
    
    setSubmitting(true);
    try {
      const response = await fetchWithAuth(API_ENDPOINTS.users.changePassword(userId), {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword
        })
      });
      
      if (response.ok) {
        toast.current?.show({
          severity: 'success',
          summary: 'Success',
          detail: 'Password changed successfully',
          life: 3000
        });
        
        // Reset form
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setErrors({});
        
        // Close dialog and notify parent
        setTimeout(() => {
          onHide();
          if (onSuccess) onSuccess();
        }, 1500);
      } else {
        const errorData = await response.json();
        const errorMessage = errorData.message || 'Failed to change password';
        
        if (errorMessage.includes('incorrect')) {
          setErrors({ currentPassword: 'Current password is incorrect' });
        } else {
          toast.current?.show({
            severity: 'error',
            summary: 'Error',
            detail: errorMessage,
            life: 3000
          });
        }
        
        handleAuthError(response, () => {});
      }
    } catch (err) {
      console.error('Error changing password:', err);
      toast.current?.show({
        severity: 'error',
        summary: 'Error',
        detail: 'An unexpected error occurred',
        life: 3000
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Toast ref={toast} position="top-right" />
      <Dialog 
        header="Change Password" 
        visible={visible} 
        onHide={onHide}
        style={{ width: '340px' }} 
        footer={null}
        dismissableMask
        closeOnEscape
        showHeader={true}
        closable={false}
      >
        <div className="flex flex-col gap-4">
          <div className="field">
            <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 mb-1">
              Current Password
            </label>
            <Password 
              id="currentPassword"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className={`w-full ${errors.currentPassword ? 'p-invalid' : ''}`}
              feedback={false}
              toggleMask
            />
            {errors.currentPassword && (
              <small className="p-error">{errors.currentPassword}</small>
            )}
          </div>
          
          <div className="field">
            <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">
              New Password
            </label>
            <Password 
              id="newPassword"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className={`w-full ${errors.newPassword ? 'p-invalid' : ''}`}
              toggleMask
              feedback={false}
            />
            {errors.newPassword && (
              <small className="p-error">{errors.newPassword}</small>
            )}
          </div>
          
          <div className="field">
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
              Confirm New Password
            </label>
            <Password 
              id="confirmPassword"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className={`w-full ${errors.confirmPassword ? 'p-invalid' : ''}`}
              feedback={false}
              toggleMask
            />
            {errors.confirmPassword && (
              <small className="p-error">{errors.confirmPassword}</small>
            )}
          </div>
          
          <div className="flex justify-center items-center mt-6 gap-4">
            <Button 
              icon="pi pi-times" 
              onClick={onHide} 
              className="p-button-text p-button-rounded" 
              disabled={submitting}
              style={{ width: '50px', height: '50px' }}
              aria-label="Cancel"
            />
            <Button 
              icon="pi pi-check" 
              onClick={handleSubmit} 
              loading={submitting}
              autoFocus
              className="p-button-rounded p-button-primary"
              style={{ width: '50px', height: '50px' }}
              aria-label="Change Password"
            />
          </div>
        </div>
      </Dialog>
    </>
  );
};

export default ChangePasswordDialog; 