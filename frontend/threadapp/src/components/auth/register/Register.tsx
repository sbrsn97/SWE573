import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import React, { useEffect, useState } from 'react'
import "./Register.css"
import isEmail from 'validator/lib/isEmail';
import { useNavigate } from 'react-router';
import { API_ENDPOINTS } from '../../../config/config';

function Register() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [emailInvalid, setEmailInvalid] = useState(false);
  const [error, setError] = useState('');

  const handleRegister = async () => {
    // Reset error state
    setError('');

    // Validate all fields are filled
    if(username === "" || password === "" || firstName === "" || lastName === "" || email === "") {
      setError('All fields are required');
      return;
    }

    // Validate email format
    if(!isEmail(email)) {
      setError('Please enter a valid email address');
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
        navigate("/");
      } else {
        setError(data.message || 'Registration failed');
      }
    } catch (err) {
      setError('Failed to connect to the server');
      console.error('Registration error:', err);
    }
  }

  useEffect(() => {
    setEmailInvalid(!isEmail(email) && email !== "");
  }, [email]);

  return (
    <div className='flex flex-col gap-2 w-[500px] h-[300px]'>
      {error && <div className="text-red-500 mb-2">{error}</div>}
      <InputText value={username} onChange={(e) => setUsername(e.target.value)} placeholder='Username'/>
      <div className='flex gap-2'>
        <InputText value={firstName} onChange={(e) => setFirstName(e.target.value)} placeholder='First Name' className='fit-this'/>
        <InputText value={lastName} onChange={(e) => setLastName(e.target.value)} placeholder='Last Name' className='fit-this'/>
      </div>
      <InputText value={email} invalid={emailInvalid} onChange={(e) => setEmail(e.target.value)} placeholder='Email' keyfilter="email"/>
      <InputText value={password} onChange={(e) => setPassword(e.target.value)} placeholder='Password' type='password'/>
      <Button label='Register' onClick={handleRegister} />
    </div>
  )
}

export default Register