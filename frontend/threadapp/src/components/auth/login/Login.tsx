import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext'
import React, { useState } from 'react'
import "./Login.css"

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  return (
    <div className='flex flex-col gap-2'>
      <InputText value={username} onChange={(e) => setUsername(e.target.value)} placeholder='Username'/>
      <InputText value={password} onChange={(e) => setPassword(e.target.value)} placeholder='Password' type='password'/>
      <Button label='Login' />
    </div>
    
  )
}

export default Login