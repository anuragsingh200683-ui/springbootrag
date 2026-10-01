import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    primary: { main: '#1565c0' },
    secondary: { main: '#00897b' },
    background: { default: '#f4f6f8' },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: [
      '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'Arial', 'sans-serif',
    ].join(','),
  },
})

export default theme
