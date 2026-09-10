import { defineConfig, type ProxyOptions } from 'vite';
import { isIP } from 'node:net';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const ambienteHttps = mode === 'preproduction' || mode === 'production';
  const proxyApi: ProxyOptions = {
    target: 'http://127.0.0.1:8081',
    changeOrigin: false,
    xfwd: false,
    rewrite: (caminho: string) => caminho.replace(/^\/api/, ''),
    headers: ambienteHttps
      ? {
          'X-Forwarded-Proto': 'https',
          'X-Forwarded-Port': '443',
        }
      : {
          'X-Forwarded-Proto': 'http',
        },
    configure: proxy => {
      proxy.on('proxyReq', (proxyReq, request) => {
        const remoto = request.socket.remoteAddress || '127.0.0.1';
        const proxyLocal = ['127.0.0.1', '::1', '::ffff:127.0.0.1'].includes(remoto);
        const ipCloudflare = request.headers['cf-connecting-ip'];
        // O tunnel local recebe CF-Connecting-IP sobrescrito pela Cloudflare.
        // Nenhum X-Forwarded-* enviado pelo navegador é reaproveitado.
        const ip = ambienteHttps && proxyLocal && typeof ipCloudflare === 'string'
          && isIP(ipCloudflare) !== 0 ? ipCloudflare : remoto;
        proxyReq.removeHeader('Forwarded');
        proxyReq.removeHeader('X-Forwarded-Prefix');
        proxyReq.removeHeader('X-Forwarded-Host');
        proxyReq.removeHeader('X-Forwarded-Server');
        proxyReq.setHeader('X-Forwarded-For', ip);
        proxyReq.setHeader('X-Real-IP', ip);
        proxyReq.setHeader('X-Forwarded-Proto', ambienteHttps ? 'https' : 'http');
        proxyReq.setHeader('X-Forwarded-Port', ambienteHttps ? '443' : '5173');
      });
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
