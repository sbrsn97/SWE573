import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaHome, FaSearch, FaCog, FaSignOutAlt } from 'react-icons/fa';
import React from 'react';

interface User {
  username: string;
  profilePicture?: string;
}

interface NavbarProps {
  user: User;
}

const Navbar = ({ user }: NavbarProps) => {
  const [showSettings, setShowSettings] = useState(false);
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/auth/logout', {
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

  return (
    <nav className="fixed top-0 left-0 right-0 h-[60px] bg-white shadow-sm z-50 px-4 py-2 flex items-center justify-between">
      <div className="flex-none">
        <Link to="/" className="p-2 rounded-full text-blue-600 hover:bg-blue-50 transition-colors">
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

      <div className="flex-none relative">
        <div className="relative">
          <img
            src={user?.profilePicture || 'https://via.placeholder.com/40'}
            alt="Profile"
            className="w-10 h-10 rounded-full cursor-pointer object-cover border-2 border-blue-600"
            onClick={() => setShowSettings(!showSettings)}
          />
          
          {showSettings && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-1">
              <Link 
                to="/profile" 
                className="flex items-center px-4 py-2 text-gray-800 hover:bg-gray-100 transition-colors"
              >
                <FaCog className="mr-3 text-gray-500" />
                Profile Settings
              </Link>
              <button 
                onClick={handleLogout} 
                className="w-full flex items-center px-4 py-2 text-gray-800 hover:bg-gray-100 transition-colors"
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