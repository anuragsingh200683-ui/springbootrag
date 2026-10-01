import { AppBar, Box, Button, Chip, Toolbar, Typography } from '@mui/material'
import LogoutIcon from '@mui/icons-material/Logout'
import { getUsername, isAdmin, logout } from '../auth/keycloak.js'

export default function Header({ drawerWidth }) {
  return (
    <AppBar
      position="fixed"
      sx={{ width: `calc(100% - ${drawerWidth}px)`, ml: `${drawerWidth}px` }}
    >
      <Toolbar>
        <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
          Employee Management System
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Typography variant="body2">{getUsername()}</Typography>
          {isAdmin() && <Chip label="Admin" size="small" color="secondary" />}
          <Button color="inherit" startIcon={<LogoutIcon />} onClick={logout}>
            Log out
          </Button>
        </Box>
      </Toolbar>
    </AppBar>
  )
}
