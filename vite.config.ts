import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // En développement, l'API Spring Boot tourne à part (port 8095).
    proxy: { '/api': 'http://localhost:8095' },
  },
});
