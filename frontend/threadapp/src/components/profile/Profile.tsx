import React, { useEffect, useState } from 'react'
import { Avatar } from 'primereact/avatar';
import { Skeleton } from 'primereact/skeleton';
import { Card } from 'primereact/card';
import { Calendar } from 'primereact/calendar';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Button } from 'primereact/button';
import { useNavigate } from 'react-router';
import Navbar from '../layout/Navbar';
import { API_ENDPOINTS } from '../../config/config';
import 'primeicons/primeicons.css';

interface User {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  role: string;
  initials: string;
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
  const navigate = useNavigate();

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

  const handleSave = async () => {
    if (!editingField || !user) return;

    try {
      const response = await fetch(API_ENDPOINTS.users.me, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ...user,
          [editingField.name]: editingField.value
        })
      });

      if (response.ok) {
        const data = await response.json();
        setUser({...data.data, initials: data.data.firstName.charAt(0) + data.data.lastName.charAt(0)});
      }
    } catch (error) {
      console.error('Error updating user:', error);
    }

    setEditingField(null);
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
      const data = await response.json();
      setUser({...data.data, initials: data.data.firstName.charAt(0) + data.data.lastName.charAt(0)});
      setLoading(false);
    }
    fetchUser();
  }, [navigate]);

  if (!user) {
    return null;
  }

  const renderEditableField = (label: string, name: EditableField['name'], value: string) => {
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
                />
              ) : name === 'birthDate' ? (
                <Calendar
                  value={editingField.value ? new Date(editingField.value) : null}
                  onChange={(e) => setEditingField({ ...editingField, value: e.value?.toISOString() || '' })}
                  dateFormat="dd.mm.yy"
                  showIcon
                />
              ) : (
                <InputText
                  value={editingField.value}
                  onChange={(e) => setEditingField({ ...editingField, value: e.target.value })}
                  className="flex-1"
                />
              )}
              <div className="flex gap-2">
                <Button icon="pi pi-check" rounded outlined severity="success" onClick={handleSave} />
                <Button icon="pi pi-times" rounded outlined severity="danger" onClick={handleCancel} />
              </div>
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

  return (
    <div className="min-h-screen bg-white">
      <Navbar user={user} />
      <div className="flex justify-center items-center min-h-[calc(100vh-60px)] mt-[60px]">
        <Card className="w-full max-w-2xl shadow-lg mx-4">
          <div className="flex flex-col gap-6">
            <div className="text-3xl font-bold text-gray-800 mb-4">Profile</div>
            
            {loading ? (
              <div className="flex flex-col gap-4">
                <div className="flex items-center gap-8">
                  <Skeleton shape="circle" size="5rem"></Skeleton>
                  <Skeleton shape="rectangle" width="30%" height="2rem"></Skeleton>
                </div>
                
                <div className="flex flex-col gap-2">
                  <Skeleton shape="rectangle" width="50%" height="2rem"></Skeleton>
                  <Skeleton shape="rectangle" width="50%" height="2rem"></Skeleton>
                  <Skeleton shape="rectangle" width="50%" height="2rem"></Skeleton>
                </div>
              </div>
            ) : (
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
                  {renderEditableField('Birth Date', 'birthDate', user.birthDate || '')}
                  {renderEditableField('Bio', 'bio', user.bio || '')}
                  {renderEditableField('Location', 'location', user.location || '')}
                  {renderEditableField('Profession', 'profession', user.profession || '')}
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
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}

export default Profile