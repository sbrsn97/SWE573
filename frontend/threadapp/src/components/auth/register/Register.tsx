import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { useEffect, useState } from 'react'
import "./Register.css"
import isEmail from 'validator/lib/isEmail';
import { API_ENDPOINTS } from '../../../config/config';

interface RegisterProps {
  onRegisterSuccess: () => void;
}

function Register({ onRegisterSuccess }: RegisterProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [emailInvalid, setEmailInvalid] = useState(false);
  const [error, setError] = useState('');
  const [showError, setShowError] = useState(false);

  const clearForm = () => {
    setUsername('');
    setPassword('');
    setFirstName('');
    setLastName('');
    setEmail('');
    setEmailInvalid(false);
    setError('');
    setShowError(false);
  };

  const handleRegister = async () => {
    // Reset error state
    setError('');
    setShowError(false);

    // Validate all fields are filled
    if(username === "" || password === "" || firstName === "" || lastName === "" || email === "") {
      setError('All fields are required');
      setShowError(true);
      setTimeout(() => setShowError(false), 1000);
      return;
    }

    // Validate email format
    if(!isEmail(email)) {
      setError('Please enter a valid email address');
      setShowError(true);
      setTimeout(() => setShowError(false), 1000);
      return;
    }

    try {
      const response = await fetch(API_ENDPOINTS.auth.register, {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: username,
          password: password,
          firstName: firstName,
          lastName: lastName,
          email: email,
        })
      });

      const data = await response.json();
      
      if(data.success) {
        clearForm();
        onRegisterSuccess();
      } else {
        setError(data.message || 'Registration failed');
        setShowError(true);
        setTimeout(() => setShowError(false), 1000);
      }
    } catch (err) {
      setError('Failed to connect to the server');
      setShowError(true);
      setTimeout(() => setShowError(false), 1000);
      console.error('Registration error:', err);
    }
  }

  useEffect(() => {
    setEmailInvalid(!isEmail(email) && email !== "");
  }, [email]);

  return (
    <div className='register-container'>
      {error && <div className="error-message">{error}</div>}
      <InputText 
        value={username} 
        onChange={(e) => setUsername(e.target.value)} 
        placeholder='Username'
        className={showError ? 'shake error-border' : ''}
      />
      <div className='flex gap-2'>
        <InputText 
          value={firstName} 
          onChange={(e) => setFirstName(e.target.value)} 
          placeholder='First Name' 
          className={`fit-this ${showError ? 'shake error-border' : ''}`}
        />
        <InputText 
          value={lastName} 
          onChange={(e) => setLastName(e.target.value)} 
          placeholder='Last Name' 
          className={`fit-this ${showError ? 'shake error-border' : ''}`}
        />
      </div>
      <InputText 
        value={email} 
        invalid={emailInvalid} 
        onChange={(e) => setEmail(e.target.value)} 
        placeholder='Email' 
        keyfilter="email"
        className={showError ? 'shake error-border' : ''}
      />
      <InputText 
        value={password} 
        onChange={(e) => setPassword(e.target.value)} 
        placeholder='Password' 
        type='password'
        className={showError ? 'shake error-border' : ''}
      />
      <Button label='Register' onClick={handleRegister} />
    </div>
  )
}

export default Register