import { useCallback, useEffect, useState } from 'react'
import AuthContext from './AuthContext.js'
import authService from '../services/authService.js'
import tokenStorage from '../utils/tokenStorage.js'

function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    if (!tokenStorage.getAccessToken()) {
      setLoading(false)
      return
    }

    authService
      .getCurrentUser()
      .then(current => {
        if (!cancelled) setUser(current)
      })
      .catch(() => {
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    function handleForcedLogout() {
      setUser(null)
    }
    window.addEventListener('auth:logout', handleForcedLogout)
    return () => window.removeEventListener('auth:logout', handleForcedLogout)
  }, [])

  const login = useCallback(async (email, password) => {
    const loggedInUser = await authService.login(email, password)
    setUser(loggedInUser)
    return loggedInUser
  }, [])

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  const refreshCurrentUser = useCallback(async () => {
    const current = await authService.getCurrentUser()
    setUser(current)
    return current
  }, [])

  const hasRole = useCallback(role => Boolean(user?.roles?.includes(role)), [user])

  const hasAnyRole = useCallback(
    roles => Boolean(user) && roles.some(role => user.roles.includes(role)),
    [user],
  )

  const value = {
    user,
    loading,
    isAuthenticated: Boolean(user),
    login,
    logout,
    refreshCurrentUser,
    hasRole,
    hasAnyRole,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
