import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import employeeService from '../services/employeeService.js'

const EMPTY_FORM = { firstName: '', lastName: '', email: '' }

function validate(form) {
  const errors = {}
  if (!form.firstName.trim()) errors.firstName = 'First name is required.'
  if (!form.lastName.trim()) errors.lastName = 'Last name is required.'
  if (!form.email.trim()) {
    errors.email = 'Email is required.'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = 'Please enter a valid email address.'
  }
  return errors
}

function EmployeeFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [loading, setLoading] = useState(isEditing)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!isEditing) return
    employeeService
      .getById(id)
      .then(res => {
        const { firstName, lastName, email } = res.data
        setForm({ firstName, lastName, email })
      })
      .catch(() => {
        setServerError('Could not load employee data. It may no longer exist.')
      })
      .finally(() => setLoading(false))
  }, [id, isEditing])

  const handleChange = e => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
    if (fieldErrors[name]) {
      setFieldErrors(prev => ({ ...prev, [name]: undefined }))
    }
  }

  const handleSubmit = async e => {
    e.preventDefault()
    setServerError(null)

    const errors = validate(form)
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      return
    }

    try {
      setSubmitting(true)
      if (isEditing) {
        await employeeService.update(id, form)
      } else {
        await employeeService.create(form)
      }
      navigate('/employees')
    } catch (err) {
      const data = err.response?.data
      if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        setFieldErrors(data.fieldErrors)
      } else {
        setServerError(
          data?.message || 'An error occurred. Please try again.',
        )
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading…</span>
        </div>
      </div>
    )
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-6 col-lg-5">
        <div className="card shadow-sm">
          <div className="card-header bg-white py-3">
            <h4 className="mb-0">
              {isEditing ? 'Edit Employee' : 'Add Employee'}
            </h4>
          </div>
          <div className="card-body p-4">
            {serverError && (
              <div className="alert alert-danger" role="alert">
                {serverError}
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
                  placeholder="Enter first name"
                  disabled={submitting}
                />
                {fieldErrors.firstName && (
                  <div className="invalid-feedback">{fieldErrors.firstName}</div>
                )}
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
                  placeholder="Enter last name"
                  disabled={submitting}
                />
                {fieldErrors.lastName && (
                  <div className="invalid-feedback">{fieldErrors.lastName}</div>
                )}
              </div>

              <div className="mb-4">
                <label htmlFor="email" className="form-label fw-semibold">
                  Email Address
                </label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  className={`form-control${fieldErrors.email ? ' is-invalid' : ''}`}
                  value={form.email}
                  onChange={handleChange}
                  placeholder="Enter email address"
                  disabled={submitting}
                />
                {fieldErrors.email && (
                  <div className="invalid-feedback">{fieldErrors.email}</div>
                )}
              </div>

              <div className="d-flex gap-2">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <span
                        className="spinner-border spinner-border-sm me-2"
                        role="status"
                        aria-hidden="true"
                      />
                      Saving…
                    </>
                  ) : isEditing ? (
                    'Update Employee'
                  ) : (
                    'Add Employee'
                  )}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => navigate('/employees')}
                  disabled={submitting}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

export default EmployeeFormPage
