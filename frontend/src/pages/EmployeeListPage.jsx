import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import employeeService from '../services/employeeService.js'

function EmployeeListPage() {
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    employeeService
      .getAll()
      .then(res => {
        if (!cancelled) setEmployees(res.data)
      })
      .catch(() => {
        if (!cancelled)
          setError('Failed to load employees. Ensure the backend is running.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleDelete = async (id, fullName) => {
    if (!window.confirm(`Delete ${fullName}? This action cannot be undone.`)) return
    try {
      setDeletingId(id)
      await employeeService.delete(id)
      setEmployees(prev => prev.filter(emp => emp.id !== id))
    } catch {
      setError('Failed to delete employee. Please try again.')
    } finally {
      setDeletingId(null)
    }
  }

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading employees…</span>
        </div>
      </div>
    )
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">Employees</h2>
        <button
          className="btn btn-primary"
          onClick={() => navigate('/employees/new')}
        >
          + Add Employee
        </button>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible" role="alert">
          {error}
          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setError(null)}
          />
        </div>
      )}

      {employees.length === 0 ? (
        <div className="text-center py-5 text-muted">
          <p className="fs-5 mb-1">No employees found.</p>
          <p className="mb-3">Get started by adding the first employee.</p>
          <button
            className="btn btn-outline-primary"
            onClick={() => navigate('/employees/new')}
          >
            Add Employee
          </button>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-striped table-hover align-middle mb-0">
            <thead className="table-dark">
              <tr>
                <th scope="col">ID</th>
                <th scope="col">First Name</th>
                <th scope="col">Last Name</th>
                <th scope="col">Email</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {employees.map(emp => (
                <tr key={emp.id}>
                  <td>{emp.id}</td>
                  <td>{emp.firstName}</td>
                  <td>{emp.lastName}</td>
                  <td>{emp.email}</td>
                  <td>
                    <button
                      className="btn btn-sm btn-outline-primary me-2"
                      onClick={() => navigate(`/employees/${emp.id}/edit`)}
                    >
                      Edit
                    </button>
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() =>
                        handleDelete(emp.id, `${emp.firstName} ${emp.lastName}`)
                      }
                      disabled={deletingId === emp.id}
                    >
                      {deletingId === emp.id ? (
                        <>
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                          Deleting…
                        </>
                      ) : (
                        'Delete'
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default EmployeeListPage
