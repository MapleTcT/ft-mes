import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    base: './',
    server: {
      host: '127.0.0.1',
      port: 4173,
      strictPort: true,
      proxy: {
        '/bpi-api': {
          target: env.BPI_API_TARGET || 'http://127.0.0.1:19090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/bpi-api/, '/bpi/v1'),
        },
      },
    },
  };
});
