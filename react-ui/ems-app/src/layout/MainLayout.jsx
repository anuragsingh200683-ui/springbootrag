import { Outlet } from 'react-router-dom'
import { Box, Toolbar } from '@mui/material'
import Sidebar from './Sidebar.jsx'
import Header from './Header.jsx'

const DRAWER_WIDTH = 240

export default function MainLayout() {
  return (
    <Box sx={{ display: 'flex' }}>
      <Header drawerWidth={DRAWER_WIDTH} />
      <Sidebar drawerWidth={DRAWER_WIDTH} />
      <Box
        component="main"
        sx={{ flexGrow: 1, p: 3, bgcolor: 'background.default', minHeight: '100vh' }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  )
}
