const ACCESS_TOKEN_KEY = 'auth.accessToken'
const REFRESH_TOKEN_KEY = 'auth.refreshToken'

// Tokens live in localStorage rather than an httpOnly cookie, since this app has
// no backend-set-cookie infrastructure. That is a deliberate tradeoff: any
// successful XSS on this origin can read both tokens. It is mitigated by short
// access-token lifetimes, rotating + server-revocable refresh tokens (hashed at
// rest), and React's default JSX escaping - but it is not as strong as httpOnly
// cookies + SameSite. Do not relax those server-side mitigations without
// reconsidering this storage choice.
const tokenStorage = {
  getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  },

  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },

  setTokens(accessToken, refreshToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },

  clearTokens() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}

export default tokenStorage
