import React, { useEffect, useState } from 'react'
import { Avatar } from 'primereact/avatar';
import { Skeleton } from 'primereact/skeleton';
import { Card } from 'primereact/card';
import { Calendar } from 'primereact/calendar';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Button } from 'primereact/button';
import { useNavigate } from 'react-router';
import { Toast } from 'primereact/toast';
import Navbar from '../layout/Navbar';
import { API_ENDPOINTS } from '../../config/config';
import MainLayout from '../layout/MainLayout';
import 'primeicons/primeicons.css';

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
  followers?: any[];
  following?: any[];
}

interface EditableField {
  name: 'bio' | 'location' | 'profession' | 'birthDate';
  value: string;
}

function Profile() {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [editingField, setEditingField] = useState<EditableField | null>(null);
  const [updating, setUpdating] = useState(false);
  const navigate = useNavigate();
  const toast = React.useRef<Toast>(null);

  const formatDate = (dateString?: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('tr-TR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const handleEdit = (name: EditableField['name'], value: string) => {
    setEditingField({ name, value });
  };

  const handleSave = async (user: User) => {
    if (!editingField) return;
    
    setUpdating(true);
    try {
      const updatePayload = {
        ...user,
        [editingField.name]: editingField.value
      };

      delete updatePayload.initials;
      delete updatePayload.followers;
      delete updatePayload.following;

      const response = await fetch(API_ENDPOINTS.users.update(user.id), {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(updatePayload)
      });

      if (!response.ok) {
        throw new Error('Failed to update profile');
      }

      const { data } = await response.json();
      
      toast.current?.show({
        severity: 'success',
        summary: 'Success',
        detail: `${editingField.name.charAt(0).toUpperCase() + editingField.name.slice(1)} updated successfully`,
        life: 3000
      });

      setEditingField(null);
    } catch (error) {
      console.error('Error updating user:', error);
      toast.current?.show({
        severity: 'error',
        summary: 'Error',
        detail: error instanceof Error ? error.message : 'Failed to update profile',
        life: 5000
      });
    } finally {
      setUpdating(false);
    }
  };

  const handleCancel = () => {
    setEditingField(null);
  };

  useEffect(() => {
    const token = localStorage.getItem('token');
    
    if (!token) {
      navigate('/auth');
      return;
    }
    const fetchUser = async () => {
      const response = await fetch(API_ENDPOINTS.users.me, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        if (response.status === 401 && errorData.code === 'TOKEN_EXPIRED') {
          localStorage.removeItem('token');
          navigate('/auth');
          return;
        }
        navigate('/auth');
        return;
      }
      
      const data = await response.json();
      setUser({...data.data, initials: data.data.firstName.charAt(0) + data.data.lastName.charAt(0)});
      setLoading(false);
    }
    fetchUser();
  }, [navigate]);

  if (!user) {
    return null;
  }

  const renderEditableField = (label: string, name: EditableField['name'], value: string, user: User) => {
    const isEditing = editingField?.name === name;
    
    return (
      <div className="flex items-start gap-4 py-2">
        <span className="font-semibold text-gray-700 min-w-[120px] pt-1">{label}:</span>
        <div className="flex-1 flex items-start gap-2">
          {isEditing ? (
            <div className="flex-1 flex items-center gap-2">
              {name === 'bio' ? (
                <InputTextarea
                  value={editingField.value}
                  onChange={(e) => setEditingField({ ...editingField, value: e.target.value })}
                  rows={3}
                  className="flex-1"
                  disabled={updating}
                />
              ) : name === 'birthDate' ? (
                <div className="flex-1 flex justify-between items-center">
                  <Calendar
                    value={editingField.value ? new Date(editingField.value) : null}
                    onChange={(e) => {
                      if (e.value) {
                        const selectedDate = new Date(e.value);
                        const year = selectedDate.getFullYear();
                        const month = selectedDate.getMonth();
                        const day = selectedDate.getDate();
                        const dateString = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                        setEditingField({ ...editingField, value: dateString });
                      } else {
                        setEditingField({ ...editingField, value: '' });
                      }
                    }}
                    dateFormat="dd.mm.yy"
                    showIcon
                    disabled={updating}
                  />
                  <div className="flex gap-2">
                    <Button 
                      icon="pi pi-check" 
                      rounded 
                      outlined 
                      severity="success" 
                      onClick={() => handleSave(user)}
                      loading={updating}
                      disabled={updating}
                    />
                    <Button 
                      icon="pi pi-times" 
                      rounded 
                      outlined 
                      severity="danger" 
                      onClick={handleCancel}
                      disabled={updating}
                    />
                  </div>
                </div>
              ) : (
                <InputText
                  value={editingField.value}
                  onChange={(e) => setEditingField({ ...editingField, value: e.target.value })}
                  className="flex-1"
                  disabled={updating}
                />
              )}
              {name !== 'birthDate' && (
                <div className="flex gap-2">
                  <Button 
                    icon="pi pi-check" 
                    rounded 
                    outlined 
                    severity="success" 
                    onClick={() => handleSave(user)}
                    loading={updating}
                    disabled={updating}
                  />
                  <Button 
                    icon="pi pi-times" 
                    rounded 
                    outlined 
                    severity="danger" 
                    onClick={handleCancel}
                    disabled={updating}
                  />
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-between">
              <span className="text-gray-800">
                {name === 'birthDate' 
                  ? (value ? formatDate(value) : 'No birth date added')
                  : (value || `No ${name} added`)}
              </span>
              <Button 
                icon="pi pi-pencil" 
                rounded
                outlined
                size="small"
                onClick={() => handleEdit(name, value || '')}
              />
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderProfile = (user: User) => (
    <div className="bg-white rounded-xl shadow-sm p-8">
      <Toast ref={toast} />
      <div className="flex flex-col gap-6">
        <div className="text-3xl font-bold text-gray-800 mb-4">Profile</div>
        <div className="flex flex-col gap-6">
          <div className="flex items-center gap-8 mb-4">
            <div className="w-[5rem] h-[5rem] flex items-center justify-center">
              <Avatar label={user.initials} shape="circle" size="xlarge" />
            </div>
            
            <div className="flex items-center h-[5rem]">
              <span className="text-xl font-bold text-gray-800">
                {user.firstName} {user.lastName}
              </span>
            </div>
          </div>
          
          <div className="flex flex-col">
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Username:</span>
              <span className="text-gray-800">{user.username}</span>
            </div>
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Email:</span>
              <span className="text-gray-800">{user.email}</span>
            </div>
            {renderEditableField('Birth Date', 'birthDate', user.birthDate || '', user)}
            {renderEditableField('Bio', 'bio', user.bio || '', user)}
            {renderEditableField('Location', 'location', user.location || '', user)}
            {renderEditableField('Profession', 'profession', user.profession || '', user)}
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Following:</span>
              <span className="text-gray-800">{user.following?.length || 0}</span>
            </div>
            <div className="py-2 flex items-center gap-4">
              <span className="font-semibold text-gray-700 min-w-[120px]">Followers:</span>
              <span className="text-gray-800">{user.followers?.length || 0}</span>
            </div>
            {user.createdAt && (
              <div className="py-2 flex items-center gap-4">
                <span className="font-semibold text-gray-700 min-w-[120px]">Joined:</span>
                <span className="text-gray-800">{formatDate(user.createdAt)}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <MainLayout>
      {(user) => renderProfile(user)}
    </MainLayout>
  );
}

export default Profile