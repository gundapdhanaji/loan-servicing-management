import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server forwards every /api call to the Spring Boot backend on port 8080,
// so the browser only ever talks to one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        // The browser adds an "Origin" header (e.g. http://localhost:5174 or http://127.0.0.1:5173).
        // Spring's CORS check rejects origins it doesn't know with 403 "Invalid CORS request".
        // Through the proxy this is a same-origin call, so drop the header and skip CORS entirely.
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyReq) => {
            proxyReq.removeHeader("origin");
          });
        },
      },
    },
  },
});
