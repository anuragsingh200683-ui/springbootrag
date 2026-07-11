import React, { useState } from "react";
import { askQuestion } from "../api/api";

export default function ChatBox({ selectedDocumentId }) {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;

    const currentQuestion = question;
    setQuestion("");
    setLoading(true);
    setError(null);
    setMessages((prev) => [...prev, { role: "user", text: currentQuestion }]);

    try {
      const response = await askQuestion(selectedDocumentId, currentQuestion);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", text: response.data.answer, sources: response.data.sources, cached: response.data.cached },
      ]);
    } catch (err) {
      const message = err.response?.data?.message || "Failed to get an answer. Check that both backend services are running.";
      setError(message);
      setMessages((prev) => prev.slice(0, -1));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>3. Ask a Question</h2>
      <div className="chat-window">
        {messages.length === 0 && <p className="muted">Ask something about your uploaded document(s).</p>}
        {messages.map((m, idx) => (
          <div key={idx} className={`chat-bubble ${m.role}`}>
            <strong>{m.role === "user" ? "You" : "Claude"}:</strong> {m.text}
            {m.cached && <span className="badge">cached</span>}
            {m.sources && m.sources.length > 0 && (
              <details>
                <summary>{m.sources.length} source chunk(s)</summary>
                {m.sources.map((s, i) => (
                  <p key={i} className="source-chunk">
                    <em>Chunk {s.chunkIndex} (score {s.similarityScore}):</em> {s.chunkText.slice(0, 200)}...
                  </p>
                ))}
              </details>
            )}
          </div>
        ))}
        {loading && <p className="muted">Thinking...</p>}
      </div>
      <form onSubmit={handleAsk} className="chat-form">
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. What is the summary of this document?"
          disabled={loading}
        />
        <button type="submit" disabled={loading || !question.trim()}>
          Ask
        </button>
      </form>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
