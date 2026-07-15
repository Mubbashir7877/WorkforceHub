import { useEffect, useState, useCallback } from 'react'
import timeClockService from '../services/timeClockService.js'

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function formatDuration(minutes) {
  if (minutes == null) return '—'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function MyTimeClockPage() {
  const [status, setStatus] = useState(null)
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const fetchStatus = useCallback(() => {
    setLoading(true)
    setError(null)
    timeClockService
      .getStatus()
      .then(res => {
        setStatus(res.data)
      })
      .catch(err => {
        const msg = err.response?.data?.message || 'Failed to load time clock status.'
        setError(msg)
      })
      .finally(() => setLoading(false))
  }, [])

  const fetchSessions = useCallback(() => {
    timeClockService
      .getMySessions(0, 5)
      .then(res => setSessions(res.data.content ?? []))
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchStatus()
    fetchSessions()
  }, [fetchStatus, fetchSessions])

  const handleClockIn = async () => {
    setActionLoading(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const res = await timeClockService.clockIn()
      setSuccessMessage(res.data.message || 'Clocked in successfully.')
      fetchStatus()
      fetchSessions()
    } catch (err) {
      setError(err.response?.data?.message || 'Clock-in failed. Please try again.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleClockOut = async () => {
    setActionLoading(true)
    setError(null)
    setSuccessMessage(null)
    try {
      const res = await timeClockService.clockOut()
      setSuccessMessage(res.data.message || 'Clocked out successfully.')
      fetchStatus()
      fetchSessions()
    } catch (err) {
      setError(err.response?.data?.message || 'Clock-out failed. Please try again.')
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">My Time Clock</h2>
          <small className="text-muted">Track your work hours</small>
        </div>
        <button
          className="btn btn-outline-secondary"
          onClick={() => { fetchStatus(); fetchSessions() }}
          disabled={loading || actionLoading}
        >
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible" role="alert">
          {error}
          <button type="button" className="btn-close" aria-label="Close" onClick={() => setError(null)} />
        </div>
      )}

      {successMessage && (
        <div className="alert alert-success alert-dismissible" role="alert">
          {successMessage}
          <button type="button" className="btn-close" aria-label="Close" onClick={() => setSuccessMessage(null)} />
        </div>
      )}

      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading…</span>
          </div>
        </div>
      ) : (
        <>
          {/* Status card */}
          <div className="card mb-4">
            <div className="card-body text-center py-5">
              {status?.clockedIn ? (
                <>
                  <div className="mb-3">
                    <span className="badge bg-success fs-5 px-3 py-2">Clocked In</span>
                  </div>
                  <p className="text-muted mb-1">
                    Session started at: <strong>{formatDateTime(status.clockInTime)}</strong>
                  </p>
                  <p className="text-muted mb-4">
                    Session ID: <span className="badge bg-light text-dark border">#{status.sessionId}</span>
                  </p>
                  <button
                    className="btn btn-danger btn-lg px-4"
                    onClick={handleClockOut}
                    disabled={actionLoading}
                  >
                    {actionLoading ? (
                      <><span className="spinner-border spinner-border-sm me-2" />Clocking Out…</>
                    ) : (
                      'Clock Out'
                    )}
                  </button>
                </>
              ) : (
                <>
                  <div className="mb-3">
                    <span className="badge bg-secondary fs-5 px-3 py-2">Clocked Out</span>
                  </div>
                  <p className="text-muted mb-4">You are not currently clocked in.</p>
                  <button
                    className="btn btn-success btn-lg px-4"
                    onClick={handleClockIn}
                    disabled={actionLoading}
                  >
                    {actionLoading ? (
                      <><span className="spinner-border spinner-border-sm me-2" />Clocking In…</>
                    ) : (
                      'Clock In'
                    )}
                  </button>
                </>
              )}
            </div>
          </div>

          {/* Recent sessions */}
          {sessions.length > 0 && (
            <div className="card">
              <div className="card-header">
                <h5 className="mb-0">Recent Sessions</h5>
              </div>
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light">
                    <tr>
                      <th>Clock In</th>
                      <th>Clock Out</th>
                      <th>Duration</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sessions.map(s => {
                      const durationMs = s.clockOutTime
                        ? new Date(s.clockOutTime) - new Date(s.clockInTime)
                        : null
                      const durationMin = durationMs != null
                        ? Math.floor(durationMs / 60000)
                        : null
                      return (
                        <tr key={s.id}>
                          <td><small>{formatDateTime(s.clockInTime)}</small></td>
                          <td><small>{formatDateTime(s.clockOutTime)}</small></td>
                          <td><small>{formatDuration(durationMin)}</small></td>
                          <td>
                            <span className={`badge ${s.status === 'OPEN' ? 'bg-success' : 'bg-secondary'}`}>
                              {s.status}
                            </span>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default MyTimeClockPage
