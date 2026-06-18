import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import employeeService from '../services/employeeService.js'
import useAuth from '../hooks/useAuth.js'
import { ROLES } from '../constants/roles.js'

function EmployeeListPage() {
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [sort, setSort] = useState({ key: null, dir: 'asc' })
  const navigate = useNavigate()
  const { hasAnyRole } = useAuth()
  const canManage = hasAnyRole([ROLES.HR_ADMIN, ROLES.SYSTEM_ADMIN])

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

  const handleSort = key => {
    setSort(prev => ({ key, dir: prev.key === key && prev.dir === 'asc' ? 'desc' : 'asc' }))
  }

  const sortedEmployees = useMemo(() => {
    if (!sort.key) return employees
    return [...employees].sort((a, b) => {
      const aVal = a[sort.key]
      const bVal = b[sort.key]
      let cmp
      if (typeof aVal === 'boolean') {
        cmp = aVal === bVal ? 0 : aVal ? -1 : 1
      } else if (typeof aVal === 'number') {
        cmp = aVal - bVal
      } else {
        cmp = String(aVal ?? '').localeCompare(String(bVal ?? ''))
      }
      return sort.dir === 'asc' ? cmp : -cmp
    })
  }, [employees, sort])

  const SortIcon = ({ col }) => {
    if (sort.key !== col) return <span style={{ opacity: 0.35, marginLeft: 4 }}>⇅</span>
    return <span style={{ marginLeft: 4 }}>{sort.dir === 'asc' ? '↑' : '↓'}</span>
  }

  const thProps = key => ({
    scope: 'col',
    onClick: () => handleSort(key),
    style: { cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' },
  })

  const handleDelete = async (id, fullName) => {
    if (!window.confirm(`Deactivate ${fullName}? They will no longer appear as active.`)) return
    try {
      setDeletingId(id)
      await employeeService.delete(id)
      setEmployees(prev => prev.map(emp => (emp.id === id ? { ...emp, active: false } : emp)))
    } catch {
      setError('Failed to deactivate employee. Please try again.')
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
        {canManage && (
          <button
            className="btn btn-primary"
            onClick={() => navigate('/employees/new')}
          >
            + Add Employee
          </button>
        )}
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
          {canManage && (
            <>
              <p className="mb-3">Get started by adding the first employee.</p>
              <button
                className="btn btn-outline-primary"
                onClick={() => navigate('/employees/new')}
              >
                Add Employee
              </button>
            </>
          )}
        </div>
      ) : (
        <div className="table-responsive">
          <table className="table table-striped table-hover align-middle mb-0">
            <thead className="table-dark">
              <tr>
                <th {...thProps('id')}>ID<SortIcon col="id" /></th>
                <th {...thProps('firstName')}>First Name<SortIcon col="firstName" /></th>
                <th {...thProps('lastName')}>Last Name<SortIcon col="lastName" /></th>
                <th {...thProps('email')}>Email<SortIcon col="email" /></th>
                <th {...thProps('active')}>Status<SortIcon col="active" /></th>
                {canManage && <th scope="col">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {sortedEmployees.map(emp => (
                <tr key={emp.id}>
                  <td>{emp.id}</td>
                  <td>{emp.firstName}</td>
                  <td>{emp.lastName}</td>
                  <td>{emp.email}</td>
                  <td>
                    <span className={`badge ${emp.active ? 'bg-success' : 'bg-secondary'}`}>
                      {emp.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {canManage && (
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
                        disabled={deletingId === emp.id || !emp.active}
                      >
                        {deletingId === emp.id ? (
                          <>
                            <span
                              className="spinner-border spinner-border-sm me-1"
                              role="status"
                              aria-hidden="true"
                            />
                            Deactivating…
                          </>
                        ) : (
                          'Deactivate'
                        )}
                      </button>
                    </td>
                  )}
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
