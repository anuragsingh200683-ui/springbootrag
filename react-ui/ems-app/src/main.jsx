import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, CssBaseline } from '@mui/material'
import theme from './theme/theme.js'
import App from './App.jsx'
import { initAuth } from './auth/keycloak.js'

const root = ReactDOM.createRoot(document.getElementById('root'))

// Log in via Keycloak before rendering, so every page can assume a valid session.
// Done outside React so StrictMode's double-invoked effects can't init Keycloak twice.
initAuth()
  .then(() =>
    root.render(
      <React.StrictMode>
        <BrowserRouter>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            <App />
          </ThemeProvider>
        </BrowserRouter>
      </React.StrictMode>,
    ),
  )
  .catch(() => root.render(<p>Could not reach the login server. Is Keycloak running on port 8180?</p>))
