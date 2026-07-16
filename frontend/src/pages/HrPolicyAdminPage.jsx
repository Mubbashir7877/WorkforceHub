import { useEffect, useState, useCallback, useRef } from 'react'
import hrPolicyService from '../services/hrPolicyService.js'

const CATEGORIES = [
  'GENERAL', 'ATTENDANCE', 'LEAVE', 'BENEFITS', 'CONDUCT', 'SECURITY', 'REMOTE_WORK', 'COMPENSATION', 'OTHER',
]

const STATUS_BADGES = {
  DRAFT: 'bg-secondary',
  UPLOADED: 'bg-info text-dark',
  PROCESSING: 'bg-warning text-dark',
  READY: 'bg-success',
  FAILED: 'bg-danger',
  INACTIVE: 'bg-dark',
}

const emptyForm = { title: '', description: '', category: 'GENERAL', version: '', effectiveDate: '' }

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString()
}

function HrPolicyAdminPage() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const [categoryFilter, setCategoryFilter] = useState('')
  const [activeFilter, setActiveFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [savingForm, setSavingForm] = useState(false)

  const [busyId, setBusyId] = useState(null)
  const fileInputRefs = useRef({})

  const fetchDocuments = useCallback(() => {
    setLoading(true)
    setError(null)
    const params = { page }
    if (categoryFilter) params.category = categoryFilter
    if (activeFilter) params.active = activeFilter === 'true'
    hrPolicyService
      .list(params)
      .then(res => {
        setDocuments(res.data.content ?? [])
        setTotalPages(res.data.totalPages ?? 0)
      })
      .catch(err => setError(err.response?.data?.message || 'Failed to load policy documents.'))
      .finally(() => setLoading(false))
  }, [page, categoryFilter, activeFilter])

  useEffect(() => {
    fetchDocuments()
  }, [fetchDocuments])

  const openCreateForm = () => {
    setEditingId(null)
    setForm(emptyForm)
    setShowForm(true)
  }

  const openEditForm = doc => {
    setEditingId(doc.id)
    setForm({
      title: doc.title || '',
      description: doc.description || '',
      category: doc.category || 'GENERAL',
      version: doc.version || '',
      effectiveDate: doc.effectiveDate || '',
    })
    setShowForm(true)
  }

  const closeForm = () => {
    setShowForm(false)
    setEditingId(null)
    setForm(emptyForm)
  }

  const handleFormSubmit = async e => {
    e.preventDefault()
    if (savingForm) return
    setSavingForm(true)
    setError(null)
    try {
      const payload = { ...form, effectiveDate: form.effectiveDate || null }
      if (editingId) {
        await hrPolicyService.update(editingId, payload)
        setSuccessMessage('Policy document updated.')
      } else {
        await hrPolicyService.create(payload)
        setSuccessMessage('Policy document created. Upload a file to process it.')
      }
      closeForm()
      fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save policy document.')
    } finally {
      setSavingForm(false)
    }
  }

  const handleFileSelected = async (doc, file) => {
    if (!file) return
    setBusyId(doc.id)
    setError(null)
    try {
      await hrPolicyService.upload(doc.id, file)
      setSuccessMessage(`File uploaded for "${doc.title}". Click Process to index it.`)
      fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'File upload failed.')
    } finally {
      setBusyId(null)
      if (fileInputRefs.current[doc.id]) fileInputRefs.current[doc.id].value = ''
    }
  }

  const handleProcess = async doc => {
    setBusyId(doc.id)
    setError(null)
    try {
      await hrPolicyService.process(doc.id)
      setSuccessMessage(`"${doc.title}" processed successfully.`)
      fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'Processing failed.')
      fetchDocuments()
    } finally {
      setBusyId(null)
    }
  }

  const handleActivate = async doc => {
    setBusyId(doc.id)
    setError(null)
    try {
      await hrPolicyService.activate(doc.id)
      setSuccessMessage(`"${doc.title}" is now active and searchable by the HR Assistant.`)
      fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'Activation failed.')
    } finally {
      setBusyId(null)
    }
  }

  const handleDeactivate = async doc => {
    if (!window.confirm(`Deactivate "${doc.title}"? It will no longer be used by the HR Assistant.`)) return
    setBusyId(doc.id)
    setError(null)
    try {
      await hrPolicyService.deactivate(doc.id)
      setSuccessMessage(`"${doc.title}" has been deactivated.`)
      fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'Deactivation failed.')
    } finally {
      setBusyId(null)
    }
  }

  const handleDownload = async doc => {
    try {
      await hrPolicyService.download(doc.id, doc.originalFileName)
    } catch (err) {
      setError(err.response?.data?.message || 'Download failed.')
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">HR Policy Documents</h2>
          <small className="text-muted">
            Upload, process, and manage the documents the HR Assistant can answer from.
          </small>
        </div>
        <button className="btn btn-primary" onClick={openCreateForm}>
          + New Policy Document
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

      {showForm && (
        <div className="card mb-4">
          <div className="card-header">
            <h5 className="mb-0">{editingId ? 'Edit Policy Document' : 'New Policy Document'}</h5>
          </div>
          <div className="card-body">
            <form onSubmit={handleFormSubmit}>
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">Title</label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    maxLength={255}
                    value={form.title}
                    onChange={e => setForm({ ...form, title: e.target.value })}
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Category</label>
                  <select
                    className="form-select"
                    value={form.category}
                    onChange={e => setForm({ ...form, category: e.target.value })}
                  >
                    {CATEGORIES.map(c => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label">Version</label>
                  <input
                    type="text"
                    className="form-control"
                    maxLength={50}
                    value={form.version}
                    onChange={e => setForm({ ...form, version: e.target.value })}
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Effective Date</label>
                  <input
                    type="date"
                    className="form-control"
                    value={form.effectiveDate || ''}
                    onChange={e => setForm({ ...form, effectiveDate: e.target.value })}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Description</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    maxLength={2000}
                    value={form.description}
                    onChange={e => setForm({ ...form, description: e.target.value })}
                  />
                </div>
              </div>
              <div className="mt-3 d-flex gap-2">
                <button type="submit" className="btn btn-primary" disabled={savingForm}>
                  {savingForm ? 'Saving…' : editingId ? 'Save Changes' : 'Create Draft'}
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={closeForm}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="row g-3 mb-3">
        <div className="col-md-3">
          <select
            className="form-select"
            value={categoryFilter}
            onChange={e => { setCategoryFilter(e.target.value); setPage(0) }}
          >
            <option value="">All categories</option>
            {CATEGORIES.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        <div className="col-md-3">
          <select
            className="form-select"
            value={activeFilter}
            onChange={e => { setActiveFilter(e.target.value); setPage(0) }}
          >
            <option value="">Active + Inactive</option>
            <option value="true">Active only</option>
            <option value="false">Inactive only</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading…</span>
          </div>
        </div>
      ) : documents.length === 0 ? (
        <div className="text-center py-5 text-muted">
          <p className="fs-5 mb-0">No policy documents found.</p>
        </div>
      ) : (
        <>
          <div className="table-responsive">
            <table className="table table-hover align-middle">
              <thead className="table-light">
                <tr>
                  <th>Title</th>
                  <th>Category</th>
                  <th>Version</th>
                  <th>Effective Date</th>
                  <th>Status</th>
                  <th>Active</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {documents.map(doc => {
                  const isBusy = busyId === doc.id
                  return (
                    <tr key={doc.id}>
                      <td>
                        <div className="fw-semibold">{doc.title}</div>
                        {doc.originalFileName && <small className="text-muted">{doc.originalFileName}</small>}
                      </td>
                      <td><span className="badge bg-light text-dark border">{doc.category}</span></td>
                      <td>{doc.version || '—'}</td>
                      <td>{formatDate(doc.effectiveDate)}</td>
                      <td>
                        <span className={`badge ${STATUS_BADGES[doc.processingStatus] ?? 'bg-secondary'}`}>
                          {doc.processingStatus}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${doc.active ? 'bg-success' : 'bg-secondary'}`}>
                          {doc.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td>
                        <div className="d-flex flex-wrap gap-1">
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => openEditForm(doc)}
                            disabled={isBusy}
                          >
                            Edit
                          </button>

                          <input
                            type="file"
                            accept=".pdf,.txt,.md,.docx"
                            className="d-none"
                            ref={el => { fileInputRefs.current[doc.id] = el }}
                            onChange={e => handleFileSelected(doc, e.target.files?.[0])}
                          />
                          <button
                            className="btn btn-sm btn-outline-primary"
                            onClick={() => fileInputRefs.current[doc.id]?.click()}
                            disabled={isBusy}
                          >
                            {doc.originalFileName ? 'Re-upload' : 'Upload'}
                          </button>

                          {doc.originalFileName && (
                            <button
                              className="btn btn-sm btn-outline-secondary"
                              onClick={() => handleDownload(doc)}
                              disabled={isBusy}
                            >
                              Download
                            </button>
                          )}

                          {['UPLOADED', 'FAILED', 'INACTIVE'].includes(doc.processingStatus) && (
                            <button
                              className="btn btn-sm btn-outline-warning"
                              onClick={() => handleProcess(doc)}
                              disabled={isBusy}
                            >
                              {isBusy ? 'Processing…' : doc.processingStatus === 'UPLOADED' ? 'Process' : 'Reprocess'}
                            </button>
                          )}

                          {doc.processingStatus === 'READY' && !doc.active && (
                            <button
                              className="btn btn-sm btn-success"
                              onClick={() => handleActivate(doc)}
                              disabled={isBusy}
                            >
                              Activate
                            </button>
                          )}

                          {doc.active && (
                            <button
                              className="btn btn-sm btn-outline-danger"
                              onClick={() => handleDeactivate(doc)}
                              disabled={isBusy}
                            >
                              Deactivate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <nav className="mt-3 d-flex justify-content-center">
              <ul className="pagination mb-0">
                <li className={`page-item${page === 0 ? ' disabled' : ''}`}>
                  <button className="page-link" onClick={() => setPage(p => p - 1)}>Previous</button>
                </li>
                {Array.from({ length: totalPages }, (_, i) => (
                  <li key={i} className={`page-item${page === i ? ' active' : ''}`}>
                    <button className="page-link" onClick={() => setPage(i)}>{i + 1}</button>
                  </li>
                ))}
                <li className={`page-item${page === totalPages - 1 ? ' disabled' : ''}`}>
                  <button className="page-link" onClick={() => setPage(p => p + 1)}>Next</button>
                </li>
              </ul>
            </nav>
          )}
        </>
      )}

      <div className="mt-4">
        <small className="text-muted">
          Note: documents are stored on local disk in this phase and will be migrated to Amazon S3
          in a future phase. A document only becomes searchable by the HR Assistant after it has
          been processed (status READY) and explicitly activated.
        </small>
      </div>
    </div>
  )
}

export default HrPolicyAdminPage
