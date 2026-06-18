import { useEffect, useState, useCallback } from 'react'
import activityLogService from '../services/activityLogService.js'

const EVENT_TYPES = [
  'EMPLOYEE_CREATED',
  'EMPLOYEE_UPDATED',
  'EMPLOYEE_DEACTIVATED',
  'EMPLOYEE_LINKED_TO_USER',
]

const EVENT_TYPE_BADGES = {
  EMPLOYEE_CREATED: 'bg-success',
  EMPLOYEE_UPDATED: 'bg-primary',
  EMPLOYEE_DEACTIVATED: 'bg-secondary',
  EMPLOYEE_LINKED_TO_USER: 'bg-info',
}

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function SystemActivityPage() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [eventTypeFilter, setEventTypeFilter] = useState('')

  const PAGE_SIZE = 20

  const fetchLogs = useCallback(() => {
    setLoading(true)
    setError(null)
    activityLogService
      .getLogs(page, PAGE_SIZE, eventTypeFilter || null)
      .then(res => {
        setLogs(res.data.content)
        setTotalPages(res.data.totalPages)
        setTotalElements(res.data.totalElements)
      })
      .catch(() => setError('Failed to load activity logs.'))
      .finally(() => setLoading(false))
  }, [page, eventTypeFilter])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleFilterChange = e => {
    setEventTypeFilter(e.target.value)
    setPage(0)
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">System Activity Log</h2>
          <small className="text-muted">
            Kafka-driven audit trail of HR employee operations
          </small>
        </div>
        <button className="btn btn-outline-secondary" onClick={fetchLogs} disabled={loading}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {/* Filter bar */}
      <div className="row g-3 mb-4">
        <div className="col-md-4">
          <select
            className="form-select"
            value={eventTypeFilter}
            onChange={handleFilterChange}
          >
            <option value="">All event types</option>
            {EVENT_TYPES.map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        {totalElements > 0 && (
          <div className="col-auto d-flex align-items-center text-muted small">
            {totalElements} record{totalElements !== 1 ? 's' : ''}
          </div>
        )}
      </div>

      {error && (
        <div className="alert alert-danger alert-dismissible" role="alert">
          {error}
          <button type="button" className="btn-close" aria-label="Close" onClick={() => setError(null)} />
        </div>
      )}

      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading…</span>
          </div>
        </div>
      ) : logs.length === 0 ? (
        <div className="text-center py-5 text-muted">
          <p className="fs-5 mb-1">No activity records found.</p>
          <p className="mb-0">
            Activity logs appear here after HR admins create, update, or deactivate employees.
            Make sure Kafka is running.
          </p>
        </div>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table table-striped table-hover align-middle mb-0">
              <thead className="table-dark">
                <tr>
                  <th>Time</th>
                  <th>Event Type</th>
                  <th>Actor</th>
                  <th>Roles</th>
                  <th>Entity</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <small>{formatDateTime(log.occurredAt)}</small>
                    </td>
                    <td>
                      <span className={`badge ${EVENT_TYPE_BADGES[log.eventType] ?? 'bg-dark'}`}>
                        {log.eventType}
                      </span>
                    </td>
                    <td>
                      <small>{log.actorEmail ?? '—'}</small>
                    </td>
                    <td>
                      {log.actorRoles
                        ? log.actorRoles.split(',').map(r => (
                            <span key={r} className="badge bg-primary me-1">{r}</span>
                          ))
                        : '—'}
                    </td>
                    <td>
                      <small className="text-muted">{log.entityType}</small>{' '}
                      <span className="badge bg-light text-dark border">#{log.entityId}</span>
                    </td>
                    <td>
                      <small>{log.message}</small>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <nav className="mt-3 d-flex justify-content-center">
              <ul className="pagination mb-0">
                <li className={`page-item${page === 0 ? ' disabled' : ''}`}>
                  <button className="page-link" onClick={() => setPage(p => p - 1)}>
                    Previous
                  </button>
                </li>
                {Array.from({ length: totalPages }, (_, i) => (
                  <li key={i} className={`page-item${page === i ? ' active' : ''}`}>
                    <button className="page-link" onClick={() => setPage(i)}>{i + 1}</button>
                  </li>
                ))}
                <li className={`page-item${page === totalPages - 1 ? ' disabled' : ''}`}>
                  <button className="page-link" onClick={() => setPage(p => p + 1)}>
                    Next
                  </button>
                </li>
              </ul>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

export default SystemActivityPage
