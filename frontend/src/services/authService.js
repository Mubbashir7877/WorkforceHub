import httpClient from './httpClient.js'
import tokenStorage from '../utils/tokenStorage.js'

const authService = {
  async login(email, password) {
    const { data } = await httpClient.post('/auth/login', { email, password })
    tokenStorage.setTokens(data.accessToken, data.refreshToken)
    return data.user
  },

  async logout() {
    const refreshToken = tokenStorage.getRefreshToken()
    tokenStorage.clearTokens()
    if (refreshToken) {
      try {
        await httpClient.post('/auth/logout', { refreshToken })
      } catch {
        // Best-effort: tokens are already cleared client-side either way.
      }
    }
  },

  async getCurrentUser() {
    const { data } = await httpClient.get('/users/me')
    return data
  },
}

export default authService
