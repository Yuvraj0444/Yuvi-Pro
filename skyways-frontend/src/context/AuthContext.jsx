import { createContext, useContext, useState, useCallback } from 'react'
import { authApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('skyways_user')) } catch { return null }
  })
  const [loading, setLoading] = useState(false)

  const login = useCallback(async (email, password) => {
    setLoading(true)
    try {
      const { data: apiResp } = await authApi.login({ email, password })
      const loginData = apiResp.data ?? apiResp
      const token = loginData.accessToken ?? loginData.token
      const userObj = { email, userId: loginData.userId, role: loginData.role }
      localStorage.setItem('skyways_token', token)
      localStorage.setItem('skyways_user', JSON.stringify(userObj))
      setUser(userObj)
      return { success: true }
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Login failed' }
    } finally {
      setLoading(false)
    }
  }, [])

  const register = useCallback(async (formData) => {
    setLoading(true)
    try {
      await authApi.register(formData)
      // Register doesn't return a token; auto-login after registration
      const loginResult = await login(formData.email, formData.password)
      return loginResult
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Registration failed' }
    } finally {
      setLoading(false)
    }
  }, [login])

  const logout = useCallback(() => {
    localStorage.removeItem('skyways_token')
    localStorage.removeItem('skyways_user')
    setUser(null)
  }, [])

  const isAuthenticated = !!localStorage.getItem('skyways_token')

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}