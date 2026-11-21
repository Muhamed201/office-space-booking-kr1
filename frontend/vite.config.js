import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const frontend = fileURLToPath(new URL('.', import.meta.url))
  const repository = fileURLToPath(new URL('..', import.meta.url))
  const { VITE_API_TARGET } = loadEnv(mode, frontend)
  const { APP_PORT } = loadEnv(mode, repository, 'APP_PORT')
  const proxy = {
    '/api': {
      target: VITE_API_TARGET || 'http://127.0.0.1:' + (APP_PORT || 8080),
      changeOrigin: true,
      configure(server) {
        server.on('proxyReq', (outgoing, incoming) => {
          // Браузер обращается к Vite с того же origin. Backend получает запрос через внутренний прокси.
          if (incoming.headers.origin === 'http://' + incoming.headers.host) {
            outgoing.removeHeader('Origin')
          }
        })
      }
    }
  }
  return { plugins: [react()], server: { port: 3000, strictPort: true, proxy }, preview: { proxy } }
})
