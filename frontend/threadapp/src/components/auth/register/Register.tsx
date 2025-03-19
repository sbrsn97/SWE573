import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import React, { useEffect, useState } from 'react'
import "./Register.css"
import isEmail from 'validator/lib/isEmail';
import { useNavigate } from 'react-router';

function Register() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [emailInvalid, setEmailInvalid] = useState(false);

  const handleRegister = async () => {
    if(username == "" || password == "" || firstName == "" || lastName == "" || email == "") {
      return;
    }

    if(isEmail(email)) {
      return;
    }

    await fetch('http://localhost:8080/api/auth/register', {
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
    }).then(response => response.json()).then(data => {
      if(data.success) {
        navigate("/")
      }
    })
  }

  useEffect(() => {
    setEmailInvalid(!isEmail(email) && email != "");
  }, [email]);

  
  return (
    <div className='flex flex-col gap-2 w-[500px] h-[300px]'>
      <InputText value={username} onChange={(e) => setUsername(e.target.value)} placeholder='Username'/>
      <div className='flex gap-2'>
        <InputText value={firstName} onChange={(e) => setFirstName(e.target.value)} placeholder='First Name' className='fit-this'/>
        <InputText value={lastName} onChange={(e) => setLastName(e.target.value)} placeholder='Last Name' className='fit-this'/>
      </div>
      <InputText value={email} invalid={emailInvalid} onChange={(e) => setEmail(e.target.value)} placeholder='Email' keyfilter="email"/>
      <InputText value={password} onChange={(e) => setPassword(e.target.value)} placeholder='Password' type='password'/>
      <Button label='Register' onClick={() => handleRegister()} />
    </div>
  )
}

export default Register