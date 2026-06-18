import { useState } from 'react'
import useAuth from '../hooks/useAuth.js'
import employeeService from '../services/employeeService.js'

function ProfilePage() {
  const { user, refreshCurrentUser } = useAuth()
  const linkedEmployee = user?.linkedEmployee

  const [form, setForm] = useState({
    firstName: linkedEmployee?.firstName || '',
    lastName: linkedEmployee?.lastName || '',
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  if (!user) return null

  const handleChange = e => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    if (fieldErrors[name]) setFieldErrors(prev => ({ ...prev, [name]: undefined }))
    setSuccess(false)
  }

  const handleSubmit = async e => {
    e.preventDefault()
    setServerError(null)
    setSuccess(false)
    try {
      setSubmitting(true)
      await employeeService.updateOwn(form)
      await refreshCurrentUser()
      setSuccess(true)
    } catch (err) {
      const data = err.response?.data
      if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        setFieldErrors(data.fieldErrors)
      } else {
        setServerError(data?.message || 'An error occurred. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-8 col-lg-6">
        <div className="card shadow-sm mb-4">
          <div className="card-header bg-white py-3">
            <h4 className="mb-0">My Account</h4>
          </div>
          <div className="card-body p-4">
            <dl className="row mb-0">
              <dt className="col-sm-4">Email</dt>
              <dd className="col-sm-8">{user.email}</dd>

              <dt className="col-sm-4">Status</dt>
              <dd className="col-sm-8">
                <span className={`badge ${user.enabled ? 'bg-success' : 'bg-secondary'}`}>
                  {user.enabled ? 'Enabled' : 'Disabled'}
                </span>
              </dd>

              <dt className="col-sm-4">Roles</dt>
              <dd className="col-sm-8">
                {user.roles.map(role => (
                  <span key={role} className="badge bg-primary me-1">
                    {role}
                  </span>
                ))}
              </dd>
            </dl>
          </div>
        </div>

        <div className="card shadow-sm">
          <div className="card-header bg-white py-3">
            <h4 className="mb-0">Employee Profile</h4>
          </div>
          <div className="card-body p-4">
            {!linkedEmployee ? (
              <p className="text-muted mb-0">No employee record is linked to your account yet.</p>
            ) : (
              <>
                {serverError && (
                  <div className="alert alert-danger" role="alert">
                    {serverError}
                  </div>
                )}
                {success && (
                  <div className="alert alert-success" role="alert">
                    Profile updated successfully.
                  </div>
                )}
                <form onSubmit={handleSubmit} noValidate>
                  <div className="mb-3">
                    <label htmlFor="firstName" className="form-label fw-semibold">
                      First Name
                    </label>
                    <input
                      type="text"
                      id="firstName"
                      name="firstName"
                      className={`form-control${fieldErrors.firstName ? ' is-invalid' : ''}`}
                      value={form.firstName}
                      onChange={handleChange}
                      disabled={submitting}
                    />
                    {fieldErrors.firstName && <div className="invalid-feedback">{fieldErrors.firstName}</div>}
                  </div>

                  <div className="mb-3">
                    <label htmlFor="lastName" className="form-label fw-semibold">
                      Last Name
                    </label>
                    <input
                      type="text"
                      id="lastName"
                      name="lastName"
                      className={`form-control${fieldErrors.lastName ? ' is-invalid' : ''}`}
                      value={form.lastName}
                      onChange={handleChange}
                      disabled={submitting}
                    />
                    {fieldErrors.lastName && <div className="invalid-feedback">{fieldErrors.lastName}</div>}
                  </div>

                  <div className="mb-3">
                    <label className="form-label fw-semibold">Email</label>
                    <input type="email" className="form-control" value={linkedEmployee.email} disabled />
                    <div className="form-text">Email cannot be changed here.</div>
                  </div>

                  <button type="submit" className="btn btn-primary" disabled={submitting}>
                    {submitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                        Saving…
                      </>
                    ) : (
                      'Save Changes'
                    )}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ProfilePage
