import { ReactNode, useEffect, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { isAuthenticated } from '../../utils/authUtils';

interface AuthRouteProps {
  children: ReactNode;
}

/**
 * AuthRoute - A wrapper component that protects routes which require authentication
 * If the user is not authenticated, they will be redirected to the login page
 */
const AuthRoute = ({ children }: AuthRouteProps) => {
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Check if user is authenticated
    const checkAuth = async () => {
      const isAuth = isAuthenticated();
      setAuthenticated(isAuth);
      setLoading(false);
    };

    checkAuth();
  }, []);

  if (loading) {
    // Optional: return a loading indicator here
    return null;
  }

  if (!authenticated) {
    // Redirect to login page and save the current location for redirecting back after login
    return <Navigate to="/auth" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default AuthRoute; 