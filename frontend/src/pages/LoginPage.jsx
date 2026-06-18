import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'

function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (isAuthenticated) {
    const redirectTo = location.state?.from?.pathname || '/profile'
    return <Navigate to={redirectTo} replace />
  }

  const handleSubmit = async e => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      const redirectTo = location.state?.from?.pathname || '/profile'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to sign in. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-6 col-lg-4">
        <div className="card shadow-sm">
          <div className="card-header bg-white py-3">
            <h4 className="mb-0">Sign In</h4>
          </div>
          <div className="card-body p-4">
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              <div className="mb-3">
                <label htmlFor="email" className="form-label fw-semibold">
                  Email Address
                </label>
                <input
                  type="email"
                  id="email"
                  className="form-control"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="Enter your email"
                  disabled={submitting}
                  autoFocus
                />
              </div>

              <div className="mb-4">
                <label htmlFor="password" className="form-label fw-semibold">
                  Password
                </label>
                <input
                  type="password"
                  id="password"
                  className="form-control"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  disabled={submitting}
                />
              </div>

              <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
                {submitting ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                    Signing in…
                  </>
                ) : (
                  'Sign In'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage
