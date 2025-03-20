import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext'
import React, { useState } from 'react'
import { useNavigate } from 'react-router';

function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = async () => {
    if(username == "" || password == "") {
      return;
    }
    await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: username,
        password: password,
      })
    }).then(response => response.json()).then(data => {
      console.log(data)
      if(data.success) {
        
        //navigate("/")
      }
    })
  }
  
  return (
    <div className='flex flex-col gap-2 w-[500px] h-[300px]'>
      <InputText value={username} onChange={(e) => setUsername(e.target.value)} placeholder='Username'/>
      <InputText value={password} onChange={(e) => setPassword(e.target.value)} placeholder='Password' type='password'/>
      <Button label='Login' onClick={() => handleLogin()} />
    </div>
  )
}

export default Login