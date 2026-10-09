import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
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
