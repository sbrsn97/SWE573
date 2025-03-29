import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaHome, FaSearch, FaCog, FaSignOutAlt } from 'react-icons/fa';
import React from 'react';
import { API_ENDPOINTS } from '../../config/config';

interface User {
  username: string;
  firstName: string;
  lastName: string;
  profilePicture?: string;
}

interface NavbarProps {
  user: User;
}

const Navbar = ({ user }: NavbarProps) => {
  const [showSettings, setShowSettings] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowSettings(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    try {
      const response = await fetch(API_ENDPOINTS.auth.logout, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        localStorage.removeItem('token');
        navigate('/auth');
      }
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const getInitials = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return '';
  };

  return (
    <nav className="fixed top-0 left-0 right-0 h-[60px] bg-white shadow-sm z-50 px-4 py-2 flex items-center justify-between">
      <div className="flex-none">
        <Link 
          to="/home" 
          className="p-2 rounded-full text-blue-600 hover:bg-blue-100 hover:scale-110 active:scale-95 transition-all duration-200 ease-in-out flex items-center justify-center"
        >
          <FaHome className="text-2xl" />
        </Link>
      </div>

      <div className="flex-1 max-w-[600px] mx-8">
        <div className="flex items-center bg-gray-100 rounded-full px-4 py-2">
          <FaSearch className="text-gray-500 mr-2" />
          <input
            type="text"
            placeholder="Search"
            className="w-full bg-transparent border-none outline-none text-gray-800 placeholder-gray-500"
          />
        </div>
      </div>

      <div className="flex-none flex items-center gap-3" ref={menuRef}>
        <span className="text-gray-700 font-medium">{user?.username}</span>
        <div className="relative">
          <div
            className="w-10 h-10 rounded-full cursor-pointer bg-blue-600 text-white flex items-center justify-center font-semibold text-sm border-2 border-blue-600 hover:bg-blue-700 hover:border-blue-700 hover:scale-105 active:scale-95 transition-all duration-200 ease-in-out shadow-md hover:shadow-lg"
            onClick={() => setShowSettings(!showSettings)}
          >
            {getInitials()}
          </div>
          
          {showSettings && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-1 transform transition-all duration-200 ease-in-out">
              <Link 
                to="/profile" 
                className="flex items-center px-4 py-2 text-gray-800 hover:bg-blue-50 transition-colors duration-200"
              >
                <FaCog className="mr-3 text-gray-500" />
                Profile Settings
              </Link>
              <button 
                onClick={handleLogout} 
                className="w-full flex items-center px-4 py-2 text-gray-800 hover:bg-red-50 transition-colors duration-200"
              >
                <FaSignOutAlt className="mr-3 text-gray-500" />
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar; 