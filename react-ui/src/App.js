import React, { useEffect, useState, useCallback } from "react";
import "./App.css";
import UploadDocument from "./components/UploadDocument";
import DocumentList from "./components/DocumentList";
import ChatBox from "./components/ChatBox";
import { listDocuments } from "./api/api";
import { getUsername, isAdmin, logout } from "./auth/keycloak";

function App() {
  const [documents, setDocuments] = useState([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState(null);
  const [loadError, setLoadError] = useState(null);

  const refreshDocuments = useCallback(async () => {
    try {
      const response = await listDocuments();
      setDocuments(response.data);
      setLoadError(null);
    } catch (err) {
      setLoadError("Could not load documents. Is the Spring Boot service running on port 8080?");
    }
  }, []);

  useEffect(() => {
    refreshDocuments();
  }, [refreshDocuments]);

  const handleUploaded = (doc) => {
    setDocuments((prev) => [doc, ...prev]);
    setSelectedDocumentId(doc.documentId);
    // Re-fetch shortly after to pick up the final PROCESSED status/chunk count.
    setTimeout(refreshDocuments, 1500);
  };

  const handleDeleted = (documentId) => {
    setDocuments((prev) => prev.filter((doc) => doc.documentId !== documentId));
    setSelectedDocumentId((prev) => (prev === documentId ? null : prev));
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="user-bar">
          <span className="muted">
            Signed in as <strong>{getUsername()}</strong>
            {isAdmin() ? " (admin)" : ""}
          </span>
          <button type="button" onClick={logout}>
            Log out
          </button>
        </div>
        <h1>AI Document Q&amp;A</h1>
        <p className="muted">Upload a PDF, then ask questions answered by Claude using semantic search.</p>
      </header>

      {loadError && <p className="error">{loadError}</p>}

      <UploadDocument onUploaded={handleUploaded} />
      <DocumentList
        documents={documents}
        selectedDocumentId={selectedDocumentId}
        onSelect={setSelectedDocumentId}
        onDeleted={handleDeleted}
      />
      <ChatBox selectedDocumentId={selectedDocumentId} />
    </div>
  );
}

export default App;
