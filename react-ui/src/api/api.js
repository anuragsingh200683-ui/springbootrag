import axios from "axios";
import { getToken, login } from "../auth/keycloak";

const BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8090";

const apiClient = axios.create({
  baseURL: BASE_URL,
});

// Attach the Keycloak access token (refreshed if close to expiry) to every request.
apiClient.interceptors.request.use(async (config) => {
  config.headers.Authorization = `Bearer ${await getToken()}`;
  return config;
});

// 401 means the token was rejected (e.g. session revoked in Keycloak) - log in again.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      login();
    }
    return Promise.reject(error);
  }
);

export function uploadDocument(file, onUploadProgress) {
  const formData = new FormData();
  formData.append("file", file);
  return apiClient.post("/api/documents/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress,
  });
}

export function listDocuments() {
  return apiClient.get("/api/documents");
}

export function getDocument(documentId) {
  return apiClient.get(`/api/documents/${documentId}`);
}

export function deleteDocument(documentId) {
  return apiClient.delete(`/api/documents/${documentId}`);
}

export function askQuestion(documentId, question) {
  return apiClient.post("/api/qa/ask", { documentId, question });
}

export default apiClient;
