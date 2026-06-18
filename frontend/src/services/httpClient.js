import axios from 'axios'
import tokenStorage from '../utils/tokenStorage.js'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const httpClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

httpClient.interceptors.request.use(config => {
  const accessToken = tokenStorage.getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

let refreshPromise = null

function refreshTokens() {
  if (!refreshPromise) {
    const refreshToken = tokenStorage.getRefreshToken()
    refreshPromise = axios
      .post(`${BASE_URL}/auth/refresh`, { refreshToken })
      .then(({ data }) => {
        tokenStorage.setTokens(data.accessToken, data.refreshToken)
        return data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function forceLogout() {
  tokenStorage.clearTokens()
  window.dispatchEvent(new Event('auth:logout'))
}

httpClient.interceptors.response.use(
  response => response,
  async error => {
    const { config, response } = error

    // Auth endpoints (login/refresh/logout) handle their own 401s - never retry them here.
    if (!response || response.status !== 401 || config._retried || config.url?.includes('/auth/')) {
      return Promise.reject(error)
    }

    if (!tokenStorage.getRefreshToken()) {
      forceLogout()
      return Promise.reject(error)
    }

    config._retried = true
    try {
      const accessToken = await refreshTokens()
      config.headers.Authorization = `Bearer ${accessToken}`
      return httpClient(config)
    } catch (refreshError) {
      forceLogout()
      return Promise.reject(refreshError)
    }
  },
)

export default httpClient
