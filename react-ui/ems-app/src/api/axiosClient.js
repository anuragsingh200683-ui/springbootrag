import axios from 'axios'
import { getToken, login } from '../auth/keycloak.js'

// baseURL is relative - the Vite dev server proxy (vite.config.js) forwards
// /api/** to springboot-service on :8080. In production this is served behind
// the same origin as the API, or VITE_API_BASE_URL can be set at build time.
const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/ems',
  headers: { 'Content-Type': 'application/json' },
})

// Attach the Keycloak access token (refreshed if close to expiry) to every request.
axiosClient.interceptors.request.use(async (config) => {
  config.headers.Authorization = `Bearer ${await getToken()}`
  return config
})

// Unwrap response.data so callers get the payload directly, and normalize
// errors to a plain Error with the backend's EmsApiError.message when present.
// A 401 means the token was rejected (e.g. session revoked in Keycloak) - log in again.
axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) login()
    const message = error.response?.data?.message || error.message || 'Unexpected error'
    const fieldErrors = error.response?.data?.fieldErrors
    const normalized = new Error(message)
    normalized.fieldErrors = fieldErrors
    normalized.status = error.response?.status
    return Promise.reject(normalized)
  },
)

export default axiosClient
