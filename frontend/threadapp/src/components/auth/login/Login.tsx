import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext'
import { useState } from 'react'
import { useNavigate } from 'react-router';
import { API_ENDPOINTS } from '../../../config/config';
import { setCurrentUserId } from '../../../utils/authUtils';
import './Login.css';

function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showError, setShowError] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleLogin = async () => {
    if(username == "" || password == "") {
      setErrorMessage("Username and password are required");
      setShowError(true);
      setTimeout(() => setShowError(false), 1000);
      return;
    }

    try {
      const response = await fetch(API_ENDPOINTS.auth.login, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username,
          password: password,
        })
      });

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem('token', data.data.token);
        
        // Store the user ID for user-specific storage
        if (data.data.userId) {
          setCurrentUserId(data.data.userId);
        }
        
        navigate("/home");
      } else {
        setErrorMessage(data.message || "Invalid username or password");
        setShowError(true);
        setTimeout(() => setShowError(false), 1000);
      }
    } catch (error) {
      setErrorMessage("An error occurred during login");
      setShowError(true);
      setTimeout(() => setShowError(false), 1000);
    }
  }
  
  return (
    <div className='flex flex-col gap-2 w-[500px] h-[300px]'>
      {showError && <div className="error-message text-red-500">{errorMessage}</div>}
      <InputText 
        value={username} 
        onChange={(e) => setUsername(e.target.value)} 
        placeholder='Username'
        className={showError ? 'shake error-border' : ''}
      />
      <InputText 
        value={password} 
        onChange={(e) => setPassword(e.target.value)} 
        placeholder='Password' 
        type='password'
        className={showError ? 'shake error-border' : ''}
      />
      <Button label='Login' onClick={() => handleLogin()} />
    </div>
  )
}

export default Login