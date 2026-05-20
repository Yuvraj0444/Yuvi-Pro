import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT on every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('skyways_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Global error handling
api.interceptors.response.use(
  res => res,
  err => {
    const isAuthEndpoint = err.config?.url?.includes('/api/v1/auth/')
    const hasToken = !!localStorage.getItem('skyways_token')
    if (err.response?.status === 401 && !isAuthEndpoint && hasToken) {
      localStorage.removeItem('skyways_token')
      localStorage.removeItem('skyways_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api