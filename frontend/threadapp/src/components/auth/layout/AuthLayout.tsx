import React from 'react'
import { Outlet } from 'react-router'
import { TabView, TabPanel } from 'primereact/tabview';
import Login from '../login/Login';
import Register from '../register/Register';

function AuthLayout() {
  return (
    <div className='w-screen h-screen flex justify-center items-center'>
      <TabView>
        <TabPanel header="Login" headerClassName='text-2xl font-bold w-1/2'>
            <Login />
        </TabPanel>
        <TabPanel header="Register" headerClassName='text-2xl font-bold w-1/2'>
            <Register />
        </TabPanel>
    </TabView>
    </div>
  )
}

export default AuthLayout