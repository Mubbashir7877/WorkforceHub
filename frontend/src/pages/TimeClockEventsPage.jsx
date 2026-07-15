import { useEffect, useState, useCallback } from 'react'
import timeClockService from '../services/timeClockService.js'

const EVENT_TYPES = ['EMPLOYEE_CLOCKED_IN', 'EMPLOYEE_CLOCKED_OUT']

const EVENT_TYPE_BADGES = {
  EMPLOYEE_CLOCKED_IN: 'bg-success',
  EMPLOYEE_CLOCKED_OUT: 'bg-warning text-dark',
}

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function parseDuration(metadataJson) {
  if (!metadataJson) return null
  try {
    const meta = JSON.parse(metadataJson)
    return meta.durationMinutes != null ? Number(meta.durationMinutes) : null
  } catch {
    return null
  }
}

function formatDuration(minutes) {
  if (minutes == null) return '—'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

function TimeClockEventsPage() {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [eventTypeFilter, setEventTypeFilter] = useState('')
  const [employeeIdFilter, setEmployeeIdFilter] = useState('')

  const PAGE_SIZE = 20

  const fetchEvents = useCallback(() => {
    setLoading(true)
    setError(null)
    const params = { page, size: PAGE_SIZE }
    if (eventTypeFilter) params.eventType = eventTypeFilter
    if (employeeIdFilter && !isNaN(Number(employeeIdFilter))) {
      params.employeeId = Number(employeeIdFilter)
    }
    timeClockService
      .getEvents(params)
      .then(res => {
        setEvents(res.data.content)
        setTotalPages(res.data.totalPages)
        setTotalElements(res.data.totalElements)
      })
      .catch(() => setError('Failed to load time clock records.'))
      .finally(() => setLoading(false))
  }, [page, eventTypeFilter, employeeIdFilter])

  useEffect(() => {
    fetchEvents()
  }, [fetchEvents])

  const handleEventTypeChange = e => {
    setEventTypeFilter(e.target.value)
    setPage(0)
  }

  const handleEmployeeIdChange = e => {
    setEmployeeIdFilter(e.target.value)
    setPage(0)
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">Time Clock Records</h2>
          <small className="text-muted">
            Kafka-driven audit trail of employee clock-in/out events
          </small>
        </div>
        <button
          className="btn btn-outline-secondary"
          onClick={fetchEvents}
          disabled={loading}
        >
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {/* Filters */}
      <div className="row g-3 mb-4">
        <div className="col-md-3">
          <select
            className="form-select"
            value={eventTypeFilter}
            onChange={handleEventTypeChange}
          >
            <option value="">All event types</option>
            {EVENT_TYPES.map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
        <div className="col-md-3">
          <input
            type="text"
            className="form-control"
            placeholder="Filter by Employee ID"
            value={employeeIdFilter}
            onChange={handleEmployeeIdChange}
          />
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
      ) : events.length === 0 ? (
        <div className="text-center py-5 text-muted">
          <p className="fs-5 mb-1">No time clock records found.</p>
          <p className="mb-0">
            Records appear here after employees clock in or out. Make sure Kafka is running.
          </p>
        </div>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table table-striped table-hover align-middle mb-0">
              <thead className="table-dark">
                <tr>
                  <th>Event Time</th>
                  <th>Event Type</th>
                  <th>Employee</th>
                  <th>User Email</th>
                  <th>Session</th>
                  <th>Duration</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {events.map(ev => (
                  <tr key={ev.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <small>{formatDateTime(ev.eventTime)}</small>
                    </td>
                    <td>
                      <span className={`badge ${EVENT_TYPE_BADGES[ev.eventType] ?? 'bg-dark'}`}>
                        {ev.eventType}
                      </span>
                    </td>
                    <td>
                      <div><small className="fw-semibold">{ev.employeeFullName ?? '—'}</small></div>
                      <div><small className="text-muted">{ev.employeeEmail ?? '—'}</small></div>
                      {ev.employeeId && (
                        <span className="badge bg-light text-dark border">#{ev.employeeId}</span>
                      )}
                    </td>
                    <td><small>{ev.userEmail ?? '—'}</small></td>
                    <td>
                      {ev.sessionId != null && (
                        <span className="badge bg-light text-dark border">#{ev.sessionId}</span>
                      )}
                    </td>
                    <td>
                      <small>{formatDuration(parseDuration(ev.metadataJson))}</small>
                    </td>
                    <td><small>{ev.message ?? '—'}</small></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

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

      <div className="mt-4">
        <small className="text-muted">
          Note: MANAGER currently sees all employee time clock records. In a future phase,
          this view will be narrowed to direct reports only once team hierarchy is implemented.
        </small>
      </div>
    </div>
  )
}

export default TimeClockEventsPage
