import { useState } from 'react'
import { TabView, TabPanel } from 'primereact/tabview';
import Login from '../login/Login';
import Register from '../register/Register';

function AuthLayout() {
  const [activeIndex, setActiveIndex] = useState(0);

  return (
    <div className='w-screen h-screen flex justify-center items-center'>
      <TabView activeIndex={activeIndex} onTabChange={(e) => setActiveIndex(e.index)}>
        <TabPanel header="Login" headerClassName='text-2xl font-bold w-1/2'>
            <Login />
        </TabPanel>
        <TabPanel header="Register" headerClassName='text-2xl font-bold w-1/2'>
            <Register onRegisterSuccess={() => setActiveIndex(0)} />
        </TabPanel>
    </TabView>
    </div>
  )
}

export default AuthLayout