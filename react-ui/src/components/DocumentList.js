import React, { useState } from "react";
import { deleteDocument } from "../api/api";
import { isAdmin } from "../auth/keycloak";

export default function DocumentList({ documents, selectedDocumentId, onSelect, onDeleted }) {
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState(null);

  const handleDelete = async (doc) => {
    if (!window.confirm(`Delete "${doc.fileName}"? This cannot be undone.`)) {
      return;
    }
    setDeletingId(doc.documentId);
    setError(null);
    try {
      await deleteDocument(doc.documentId);
      onDeleted(doc.documentId);
    } catch (err) {
      const message =
        err.response?.data?.message || "Delete failed. Is the Spring Boot service running on port 8080?";
      setError(message);
    } finally {
      setDeletingId(null);
    }
  };

  if (!documents.length) {
    return (
      <div className="card">
        <h2>2. Your Documents</h2>
        <p className="muted">No documents uploaded yet.</p>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>2. Your Documents</h2>
      {error && <p className="error">{error}</p>}
      <ul className="doc-list">
        <li>
          <label>
            <input
              type="radio"
              name="document"
              checked={selectedDocumentId === null}
              onChange={() => onSelect(null)}
            />
            Search across all documents
          </label>
        </li>
        {documents.map((doc) => (
          <li key={doc.documentId}>
            <label>
              <input
                type="radio"
                name="document"
                checked={selectedDocumentId === doc.documentId}
                onChange={() => onSelect(doc.documentId)}
              />
              {doc.fileName}{" "}
              <span className={`status status-${doc.status.toLowerCase()}`}>{doc.status}</span>
              {doc.chunkCount ? <span className="muted"> ({doc.chunkCount} chunks)</span> : null}
            </label>
            {/* Deleting documents is ADMIN-only on the API; hide it from everyone else. */}
            {isAdmin() && (
              <button
                type="button"
                className="delete-btn"
                onClick={() => handleDelete(doc)}
                disabled={deletingId === doc.documentId}
              >
                {deletingId === doc.documentId ? "Deleting..." : "Delete"}
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
