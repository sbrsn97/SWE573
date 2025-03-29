import React, { useEffect, useState } from 'react'
import { Avatar } from 'primereact/avatar';
import { Skeleton } from 'primereact/skeleton';
import { useNavigate } from 'react-router';

interface User {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  role: string;
  initials: string;
}

function Profile() {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    
    if (!token) {
      navigate('/auth');
      return;
    }
    const fetchUser = async () => {
      const response = await fetch('http://localhost:8080/api/users/me', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const data = await response.json();
      setUser({...data.data, initials: data.data.firstName.charAt(0) + data.data.lastName.charAt(0)});
      setLoading(false);
    }
    fetchUser();
  }, []);
  return (
    <div className='flex justify-center w-screen p-4'>
      <div className='flex flex-col w-1/2'>
      <div className='text-4xl font-bold'>Profile</div>
        {loading ? 
        (<div className='flex flex-col gap-4'>
          <div className='flex items-center gap-8'>
            <Skeleton shape="circle" size="5rem"></Skeleton>
            <Skeleton shape="rectangle" width="30%" height="2rem" ></Skeleton>
          </div>
          
          <div className='flex flex-col gap-2'>
            <Skeleton shape="rectangle" width="50%" height="2rem" ></Skeleton>
            <Skeleton shape="rectangle" width="50%" height="2rem" ></Skeleton>
            <Skeleton shape="rectangle" width="50%" height="2rem" ></Skeleton>
          </div>
        </div>) : 
        (
        <div className='flex flex-col gap-4'>
          <div className='flex items-center gap-8'>
            <div className='w-[5rem] h-[5rem]'>
            <Avatar label={user!.initials} shape='circle' size='xlarge' />
            </div>
            
            <div className='h-[32px] flex items-center'>
              <span className='font-bold'>
                {user!.firstName} {user!.lastName}
              </span>
            </div>
          </div>
          
          <div className='flex flex-col gap-2'>
            <div className='h-[32px] flex items-center'>
              <span>
                Username: {user!.username}
              </span>
            </div>
            <div className='h-[32px] flex items-center'>
              <span>
                Email: {user!.email}
              </span>
            </div>
            <div className='h-[32px] flex items-center'>
              <span>
                Role: {user!.role}
              </span>
            </div>
          </div>
        </div>)}
      </div>
    
    </div>
    
  )
}

export default Profile