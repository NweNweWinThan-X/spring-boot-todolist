import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Cloud dev environments (Codespaces, Gitpod) serve this dev server from a generated
    // hostname rather than localhost. Vite rejects unknown Host headers by default.
    allowedHosts: ['.app.github.dev', '.github.dev', '.gitpod.io'],
    proxy: {
      // Lets the SPA call the API same-origin as `/api/...` instead of an absolute
      // http://localhost:8080 URL. That matters outside localhost: a forwarded cloud URL
      // would otherwise send an Origin the API's CORS allowlist does not contain, and every
      // request would fail. Unused unless VITE_API_BASE_URL is set to a relative path, so
      // the default local setup is unchanged.
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // Browsers attach Origin to same-origin POST/PUT/DELETE as well, so a cloud URL's
        // origin would reach the API and its CORS allowlist would answer 403. Overwrite it
        // with one the allowlist contains; past this point the hop is server to server.
        headers: { origin: 'http://localhost:5173' },
      },
    },
    watch: {
      // Playwright writes its report and traces inside this directory. Without this the
      // watcher reloads the page mid-test and the running assertion loses its DOM.
      ignored: [
        '**/playwright-report/**',
        '**/test-results/**',
        '**/blob-report/**',
        '**/dist/**',
      ],
    },
  },
});
