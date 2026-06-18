import { useEffect, useState } from 'react'
import userService from '../services/userService.js'
import { ALL_ROLES } from '../constants/roles.js'
import useAuth from '../hooks/useAuth.js'

const EMPTY_NEW_USER = { email: '', password: '', roles: [], employeeId: '' }

function UserAdminPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newUser, setNewUser] = useState(EMPTY_NEW_USER)
  const [createFieldErrors, setCreateFieldErrors] = useState({})
  const [createError, setCreateError] = useState(null)
  const [creating, setCreating] = useState(false)

  const [editingRolesId, setEditingRolesId] = useState(null)
  const [editingRoles, setEditingRoles] = useState([])
  const [rolesError, setRolesError] = useState(null)
  const [savingRoles, setSavingRoles] = useState(false)

  const [linkingId, setLinkingId] = useState(null)
  const [linkEmployeeId, setLinkEmployeeId] = useState('')
  const [linkError, setLinkError] = useState(null)

  const [busyUserId, setBusyUserId] = useState(null)

  const loadUsers = () => {
    setLoading(true)
    userService
      .list()
      .then(res => setUsers(res.data))
      .catch(() => setError('Failed to load users.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadUsers()
  }, [])

  const toggleNewUserRole = role => {
    setNewUser(prev => ({
      ...prev,
      roles: prev.roles.includes(role) ? prev.roles.filter(r => r !== role) : [...prev.roles, role],
    }))
  }

  const handleCreateSubmit = async e => {
    e.preventDefault()
    setCreateError(null)
    setCreateFieldErrors({})
    try {
      setCreating(true)
      await userService.create({
        email: newUser.email,
        password: newUser.password,
        roles: newUser.roles,
        employeeId: newUser.employeeId ? Number(newUser.employeeId) : null,
      })
      setNewUser(EMPTY_NEW_USER)
      setShowCreateForm(false)
      loadUsers()
    } catch (err) {
      const data = err.response?.data
      if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        setCreateFieldErrors(data.fieldErrors)
      } else {
        setCreateError(data?.message || 'Failed to create user.')
      }
    } finally {
      setCreating(false)
    }
  }

  const handleToggleEnabled = async user => {
    setBusyUserId(user.id)
    setError(null)
    try {
      const updated = user.enabled ? await userService.disable(user.id) : await userService.enable(user.id)
      setUsers(prev => prev.map(u => (u.id === user.id ? updated.data : u)))
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update user status.')
    } finally {
      setBusyUserId(null)
    }
  }

  const startEditRoles = user => {
    setEditingRolesId(user.id)
    setEditingRoles(user.roles)
    setRolesError(null)
  }

  const toggleEditingRole = role => {
    setEditingRoles(prev => (prev.includes(role) ? prev.filter(r => r !== role) : [...prev, role]))
  }

  const handleSaveRoles = async userId => {
    setSavingRoles(true)
    setRolesError(null)
    try {
      const updated = await userService.updateRoles(userId, editingRoles)
      setUsers(prev => prev.map(u => (u.id === userId ? updated.data : u)))
      setEditingRolesId(null)
    } catch (err) {
      setRolesError(err.response?.data?.message || 'Failed to update roles.')
    } finally {
      setSavingRoles(false)
    }
  }

  const handleRevokeTokens = async userId => {
    if (!window.confirm('Revoke all active sessions for this user?')) return
    setBusyUserId(userId)
    setError(null)
    try {
      await userService.revokeTokens(userId)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to revoke tokens.')
    } finally {
      setBusyUserId(null)
    }
  }

  const startLinkEmployee = user => {
    setLinkingId(user.id)
    setLinkEmployeeId(user.linkedEmployee?.id ? String(user.linkedEmployee.id) : '')
    setLinkError(null)
  }

  const handleLinkEmployee = async userId => {
    setLinkError(null)
    try {
      const updated = await userService.linkEmployee(userId, Number(linkEmployeeId))
      setUsers(prev => prev.map(u => (u.id === userId ? updated.data : u)))
      setLinkingId(null)
    } catch (err) {
      setLinkError(err.response?.data?.message || 'Failed to link employee.')
    }
  }

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading users…</span>
        </div>
      </div>
    )
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">User Administration</h2>
        <button className="btn btn-primary" onClick={() => setShowCreateForm(prev => !prev)}>
          {showCreateForm ? 'Cancel' : '+ Create User'}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible" role="alert">
          {error}
          <button type="button" className="btn-close" aria-label="Close" onClick={() => setError(null)} />
        </div>
      )}

      {showCreateForm && (
        <div className="card shadow-sm mb-4">
          <div className="card-body p-4">
            {createError && (
              <div className="alert alert-danger" role="alert">
                {createError}
              </div>
            )}
            <form onSubmit={handleCreateSubmit} noValidate>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <label className="form-label fw-semibold">Email</label>
                  <input
                    type="email"
                    className={`form-control${createFieldErrors.email ? ' is-invalid' : ''}`}
                    value={newUser.email}
                    onChange={e => setNewUser(prev => ({ ...prev, email: e.target.value }))}
                  />
                  {createFieldErrors.email && <div className="invalid-feedback">{createFieldErrors.email}</div>}
                </div>
                <div className="col-md-6 mb-3">
                  <label className="form-label fw-semibold">Temporary Password</label>
                  <input
                    type="password"
                    className={`form-control${createFieldErrors.password ? ' is-invalid' : ''}`}
                    value={newUser.password}
                    onChange={e => setNewUser(prev => ({ ...prev, password: e.target.value }))}
                  />
                  {createFieldErrors.password && (
                    <div className="invalid-feedback">{createFieldErrors.password}</div>
                  )}
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label fw-semibold d-block">Roles</label>
                {ALL_ROLES.map(role => (
                  <div className="form-check form-check-inline" key={role}>
                    <input
                      type="checkbox"
                      className="form-check-input"
                      id={`new-role-${role}`}
                      checked={newUser.roles.includes(role)}
                      onChange={() => toggleNewUserRole(role)}
                    />
                    <label className="form-check-label" htmlFor={`new-role-${role}`}>
                      {role}
                    </label>
                  </div>
                ))}
                {createFieldErrors.roles && <div className="text-danger small mt-1">{createFieldErrors.roles}</div>}
              </div>

              <div className="mb-3">
                <label className="form-label fw-semibold">Link Employee ID (optional)</label>
                <input
                  type="number"
                  className="form-control"
                  style={{ maxWidth: '200px' }}
                  value={newUser.employeeId}
                  onChange={e => setNewUser(prev => ({ ...prev, employeeId: e.target.value }))}
                />
              </div>

              <button type="submit" className="btn btn-primary" disabled={creating}>
                {creating ? 'Creating…' : 'Create User'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="table-responsive">
        <table className="table table-striped table-hover align-middle mb-0">
          <thead className="table-dark">
            <tr>
              <th>ID</th>
              <th>Email</th>
              <th>Status</th>
              <th>Roles</th>
              <th>Linked Employee</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.email}</td>
                <td>
                  <span className={`badge ${user.enabled ? 'bg-success' : 'bg-secondary'}`}>
                    {user.enabled ? 'Enabled' : 'Disabled'}
                  </span>
                </td>
                <td>
                  {editingRolesId === user.id ? (
                    <div>
                      {ALL_ROLES.map(role => (
                        <div className="form-check form-check-inline" key={role}>
                          <input
                            type="checkbox"
                            className="form-check-input"
                            id={`edit-role-${user.id}-${role}`}
                            checked={editingRoles.includes(role)}
                            onChange={() => toggleEditingRole(role)}
                          />
                          <label className="form-check-label" htmlFor={`edit-role-${user.id}-${role}`}>
                            {role}
                          </label>
                        </div>
                      ))}
                      {rolesError && <div className="text-danger small mt-1">{rolesError}</div>}
                      <div className="mt-2">
                        <button
                          className="btn btn-sm btn-primary me-2"
                          disabled={savingRoles}
                          onClick={() => handleSaveRoles(user.id)}
                        >
                          Save
                        </button>
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => setEditingRolesId(null)}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    user.roles.map(role => (
                      <span key={role} className="badge bg-primary me-1">
                        {role}
                      </span>
                    ))
                  )}
                </td>
                <td>
                  {linkingId === user.id ? (
                    <div className="d-flex align-items-center gap-2">
                      <input
                        type="number"
                        className="form-control form-control-sm"
                        style={{ width: '90px' }}
                        value={linkEmployeeId}
                        onChange={e => setLinkEmployeeId(e.target.value)}
                      />
                      <button className="btn btn-sm btn-primary" onClick={() => handleLinkEmployee(user.id)}>
                        Save
                      </button>
                      <button className="btn btn-sm btn-outline-secondary" onClick={() => setLinkingId(null)}>
                        Cancel
                      </button>
                      {linkError && <div className="text-danger small">{linkError}</div>}
                    </div>
                  ) : user.linkedEmployee ? (
                    `${user.linkedEmployee.firstName} ${user.linkedEmployee.lastName}`
                  ) : (
                    <span className="text-muted">None</span>
                  )}
                </td>
                <td>
                  <div className="d-flex flex-wrap gap-2">
                    <button
                      className="btn btn-sm btn-outline-secondary"
                      disabled={busyUserId === user.id || (user.enabled && user.id === currentUser.id)}
                      title={user.enabled && user.id === currentUser.id ? 'You cannot disable your own account.' : undefined}
                      onClick={() => handleToggleEnabled(user)}
                    >
                      {user.enabled ? 'Disable' : 'Enable'}
                    </button>
                    <button className="btn btn-sm btn-outline-primary" onClick={() => startEditRoles(user)}>
                      Edit Roles
                    </button>
                    <button className="btn btn-sm btn-outline-primary" onClick={() => startLinkEmployee(user)}>
                      Link Employee
                    </button>
                    <button
                      className="btn btn-sm btn-outline-danger"
                      disabled={busyUserId === user.id}
                      onClick={() => handleRevokeTokens(user.id)}
                    >
                      Revoke Sessions
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default UserAdminPage
