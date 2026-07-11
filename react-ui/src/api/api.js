import axios from "axios";

const BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";
const USERNAME = process.env.REACT_APP_API_USERNAME || "admin";
const PASSWORD = process.env.REACT_APP_API_PASSWORD || "changeme";

const apiClient = axios.create({
  baseURL: BASE_URL,
  auth: { username: USERNAME, password: PASSWORD },
});

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
