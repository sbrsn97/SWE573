import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { FaHome, FaSearch, FaSignOutAlt, FaList, FaShieldAlt, FaChevronDown, FaChevronUp, FaFilter, FaUserShield } from 'react-icons/fa';
import { API_ENDPOINTS } from '../../config/config';
import { clearAllLocalStorage } from '../../utils/authUtils';
import SearchResults from './SearchResults';

interface User {
  username: string;
  firstName: string;
  lastName: string;
  profilePicture?: string;
  role?: string;
}

interface NavbarProps {
  user: User;
}

const Navbar = ({ user }: NavbarProps) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [showSearchResults, setShowSearchResults] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const location = useLocation();

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowSearchResults(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/auth');
        return;
      }

      const response = await fetch(API_ENDPOINTS.auth.logout, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json();
        if (response.status === 401 && errorData.code === 'TOKEN_EXPIRED') {
          // Token is already expired, just clear it and redirect
          clearAllLocalStorage();
          navigate('/auth');
          return;
        }
        throw new Error('Logout failed');
      }

      // Clear token and all other storage data on successful logout
      clearAllLocalStorage();
      navigate('/auth');
    } catch (error) {
      console.error('Logout failed:', error);
      // Even if the server request fails, we should still clear the token and redirect
      clearAllLocalStorage();
      navigate('/auth');
    }
  };

  const getInitials = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return '';
  };

  const isActive = (path: string) => {
    return location.pathname === path ? 'text-blue-700 bg-blue-100' : 'text-blue-600 hover:bg-blue-100';
  };

  return (
    <nav className="fixed top-0 left-0 right-0 h-[60px] bg-white shadow-sm z-50 px-4 py-2 flex items-center justify-between">
      <div className="flex-none flex items-center">
        <Link 
          to="/home" 
          className={`p-2 rounded-full hover:scale-110 active:scale-95 transition-all duration-200 ease-in-out flex items-center justify-center mr-2 ${isActive('/home')}`}
        >
          <FaHome className="text-2xl" />
        </Link>
        <Link 
          to="/threads" 
          className={`p-2 rounded-full hover:scale-110 active:scale-95 transition-all duration-200 ease-in-out flex items-center justify-center ${isActive('/threads')}`}
        >
          <FaList className="text-2xl" />
        </Link>
      </div>

      <div className="flex-1 max-w-[600px] mx-8 relative" ref={searchRef}>
        <div className="flex items-center bg-gray-100 rounded-full px-4 py-2">
          <FaSearch className="text-gray-500 mr-2" />
          <input
            type="text"
            placeholder="Search threads and users..."
            className="w-full bg-transparent border-none outline-none text-gray-800 placeholder-gray-500"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setShowSearchResults(true);
            }}
            onFocus={() => setShowSearchResults(true)}
          />
        </div>
        {showSearchResults && searchQuery && (
          <SearchResults
            query={searchQuery}
            onClose={() => setShowSearchResults(false)}
          />
        )}
      </div>

      <div className="flex-none flex items-center gap-3">
        {isAdmin && (
          <Link
            to="/admin"
            className="w-10 h-10 rounded-full bg-blue-400 text-white flex items-center justify-center font-bold text-sm border-2 border-blue-300 hover:bg-blue-500 hover:border-blue-400 hover:scale-105 active:scale-95 transition-all duration-200 ease-in-out shadow-md hover:shadow-lg"
            title="Admin Dashboard"
          >
            <FaUserShield className="text-white text-xl" />
          </Link>
        )}
        
        <span className="text-gray-700 font-medium">{user?.username}</span>
        
        <Link 
          to="/users/me" 
          className="w-10 h-10 rounded-full bg-blue-500 text-white flex items-center justify-center font-bold text-sm border-2 border-blue-400 hover:bg-blue-600 hover:border-blue-500 hover:scale-105 active:scale-95 transition-all duration-200 ease-in-out shadow-md hover:shadow-lg"
          title="View your profile"
        >
          <span className="text-white">{getInitials()}</span>
        </Link>
        
        <button 
          onClick={handleLogout} 
          className="flex items-center justify-center p-2 rounded-full text-red-500 hover:bg-red-50 hover:scale-110 active:scale-95 transition-all duration-200 ease-in-out"
          title="Logout"
        >
          <FaSignOutAlt className="text-xl" />
        </button>
      </div>
    </nav>
  );
};

export default Navbar; 