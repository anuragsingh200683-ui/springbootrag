import { AppBar, Toolbar, Typography } from '@mui/material'

export default function Header({ drawerWidth }) {
  return (
    <AppBar
      position="fixed"
      sx={{ width: `calc(100% - ${drawerWidth}px)`, ml: `${drawerWidth}px` }}
    >
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          Employee Management System
        </Typography>
      </Toolbar>
    </AppBar>
  )
}
