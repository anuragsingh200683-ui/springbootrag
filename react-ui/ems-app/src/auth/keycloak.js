import Keycloak from 'keycloak-js'

// Login is handled entirely by the Keycloak auth server (realm 'aiapp', public client
// 'aiapp-web'): this app never sees a password. Authorization Code + PKCE redirects the
// browser to Keycloak and back with a short-lived access token, which is attached to every
// API call as 'Authorization: Bearer <token>' and validated by springboot-service.
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'aiapp',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'aiapp-web',
})

// Seconds of remaining validity below which getToken() refreshes before an API call.
const MIN_TOKEN_VALIDITY = 30

/** Redirects to the Keycloak login page if there is no session; resolves once logged in. */
export function initAuth() {
  return keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  })
}

/** A valid access token, refreshed first if it is about to expire. */
export async function getToken() {
  try {
    await keycloak.updateToken(MIN_TOKEN_VALIDITY)
  } catch {
    // Refresh token expired too (SSO session ended) - start a fresh login.
    await keycloak.login()
  }
  return keycloak.token
}

export const login = () => keycloak.login()
export const logout = () => keycloak.logout({ redirectUri: window.location.origin })
export const getUsername = () => keycloak.tokenParsed?.preferred_username
export const hasRole = (role) => keycloak.hasRealmRole(role)
export const isAdmin = () => hasRole('ADMIN')
