import React from "react";
import ReactDOM from "react-dom/client";
import "./App.css";
import App from "./App";
import { initAuth } from "./auth/keycloak";

const root = ReactDOM.createRoot(document.getElementById("root"));

// Log in via Keycloak before rendering, so every component can assume a valid session.
// Done outside React so StrictMode's double-invoked effects can't init Keycloak twice.
initAuth()
  .then(() =>
    root.render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    )
  )
  .catch(() =>
    root.render(
      <p className="error">Could not reach the login server. Is Keycloak running on port 8180?</p>
    )
  );
