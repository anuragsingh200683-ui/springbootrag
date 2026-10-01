import React, { useState } from "react";
import { uploadDocument } from "../api/api";

export default function UploadDocument({ onUploaded }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState(null);

  const handleFileChange = (e) => {
    setFile(e.target.files[0] || null);
    setError(null);
  };

  const handleUpload = async () => {
    if (!file) {
      setError("Please choose a PDF file first.");
      return;
    }
    setUploading(true);
    setError(null);
    setProgress(0);
    try {
      const response = await uploadDocument(file, (evt) => {
        setProgress(Math.round((evt.loaded * 100) / evt.total));
      });
      onUploaded(response.data);
      setFile(null);
    } catch (err) {
      const message =
        err.response?.data?.message || "Upload failed. Is the Spring Boot service running on port 8090?";
      setError(message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="card">
      <h2>1. Upload a PDF</h2>
      <input type="file" accept="application/pdf" onChange={handleFileChange} disabled={uploading} />
      <button onClick={handleUpload} disabled={uploading || !file}>
        {uploading ? `Uploading... ${progress}%` : "Upload & Process"}
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
