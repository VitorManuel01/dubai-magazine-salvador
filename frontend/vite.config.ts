import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const ambienteHttps = mode === 'preproduction' || mode === 'production';
  const proxyApi = {
    target: 'http://127.0.0.1:8081',
    changeOrigin: false,
    xfwd: true,
    rewrite: (caminho: string) => caminho.replace(/^\/api/, ''),
    headers: ambienteHttps
      ? {
          'X-Forwarded-Proto': 'https',
          'X-Forwarded-Port': '443',
        }
      : {
          'X-Forwarded-Proto': 'http',
        },
  };

  return {
    plugins: [react()],
    server: {
      host: ambienteHttps ? '127.0.0.1' : '0.0.0.0',
      port: 5173,
      strictPort: true,
      allowedHosts: ['.trycloudflare.com'],
      proxy: { '/api': proxyApi },
    },
    preview: {
      host: ambienteHttps ? '127.0.0.1' : '0.0.0.0',
      port: 4173,
      strictPort: true,
      allowedHosts: ['.trycloudflare.com'],
      proxy: { '/api': proxyApi },
    },
  };
});
