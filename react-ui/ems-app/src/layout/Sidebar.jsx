import { Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar } from '@mui/material'
import { NavLink } from 'react-router-dom'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import ApartmentIcon from '@mui/icons-material/Apartment'
import BadgeIcon from '@mui/icons-material/Badge'
import EventAvailableIcon from '@mui/icons-material/EventAvailable'
import EventNoteIcon from '@mui/icons-material/EventNote'
import SmartToyIcon from '@mui/icons-material/SmartToy'

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Employees', path: '/employees', icon: <PeopleIcon /> },
  { label: 'Departments', path: '/departments', icon: <ApartmentIcon /> },
  { label: 'Designations', path: '/designations', icon: <BadgeIcon /> },
  { label: 'Attendance', path: '/attendance', icon: <EventAvailableIcon /> },
  { label: 'Leave', path: '/leave', icon: <EventNoteIcon /> },
  { label: 'AI Assistant', path: '/ai-assistant', icon: <SmartToyIcon /> },
]

export default function Sidebar({ drawerWidth }) {
  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' },
      }}
    >
      <Toolbar />
      <List>
        {NAV_ITEMS.map((item) => (
          <ListItemButton
            key={item.path}
            component={NavLink}
            to={item.path}
            sx={{ '&.active': { bgcolor: 'action.selected' } }}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
    </Drawer>
  )
}
