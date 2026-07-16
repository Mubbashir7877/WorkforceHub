import { useEffect, useState, useCallback, useRef } from 'react'
import hrAssistantService from '../services/hrAssistantService.js'

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function SourceList({ sources }) {
  if (!sources || sources.length === 0) return null
  return (
    <div className="mt-2 pt-2 border-top">
      <small className="text-muted d-block mb-1">Sources:</small>
      <ul className="list-unstyled mb-0">
        {sources.map((s, i) => (
          <li key={i} className="small text-muted mb-1">
            <span className="badge bg-light text-dark border me-1">
              {s.title || 'Untitled Policy'}
              {s.version ? ` v${s.version}` : ''}
              {s.pageNumber != null ? `, p.${s.pageNumber}` : ''}
            </span>
            {s.category && <span className="badge bg-secondary-subtle text-secondary-emphasis">{s.category}</span>}
          </li>
        ))}
      </ul>
    </div>
  )
}

function HrAssistantPage() {
  const [conversations, setConversations] = useState([])
  const [loadingConversations, setLoadingConversations] = useState(true)
  const [selectedConversationId, setSelectedConversationId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState(null)
  const [aiDisabled, setAiDisabled] = useState(false)
  const messagesEndRef = useRef(null)

  const fetchConversations = useCallback(() => {
    setLoadingConversations(true)
    hrAssistantService
      .getConversations()
      .then(res => setConversations(res.data.content ?? []))
      .catch(() => {})
      .finally(() => setLoadingConversations(false))
  }, [])

  useEffect(() => {
    fetchConversations()
  }, [fetchConversations])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSelectConversation = id => {
    setSelectedConversationId(id)
    setError(null)
    setAiDisabled(false)
    setLoadingMessages(true)
    hrAssistantService
      .getConversation(id)
      .then(res => setMessages(res.data.messages ?? []))
      .catch(err => setError(err.response?.data?.message || 'Failed to load this conversation.'))
      .finally(() => setLoadingMessages(false))
  }

  const handleNewConversation = () => {
    setSelectedConversationId(null)
    setMessages([])
    setError(null)
    setAiDisabled(false)
  }

  const handleDeleteConversation = async (id, e) => {
    e.stopPropagation()
    if (!window.confirm('Delete this conversation? This cannot be undone.')) return
    try {
      await hrAssistantService.deleteConversation(id)
      if (selectedConversationId === id) handleNewConversation()
      fetchConversations()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete conversation.')
    }
  }

  const handleSubmit = async e => {
    e.preventDefault()
    const question = inputValue.trim()
    if (!question || sending) return

    setSending(true)
    setError(null)
    setAiDisabled(false)

    const userMessage = { role: 'USER', content: question, createdAt: new Date().toISOString() }
    setMessages(prev => [...prev, userMessage])
    setInputValue('')

    try {
      const res = await hrAssistantService.chat(question, selectedConversationId)
      const assistantMessage = {
        role: 'ASSISTANT',
        content: res.data.answer,
        grounded: res.data.grounded,
        sources: res.data.sources,
        createdAt: res.data.createdAt,
      }
      setMessages(prev => [...prev, assistantMessage])
      if (!selectedConversationId) {
        setSelectedConversationId(res.data.conversationId)
      }
      fetchConversations()
    } catch (err) {
      const code = err.response?.data?.errorCode
      if (code === 'AI_DISABLED') {
        setAiDisabled(true)
      } else {
        setError(err.response?.data?.message || 'The HR assistant could not answer your question. Please try again.')
      }
      // Roll back the optimistically-added question so the input can be retried.
      setMessages(prev => prev.filter(m => m !== userMessage))
      setInputValue(question)
    } finally {
      setSending(false)
    }
  }

  return (
    <div>
      <div className="mb-4">
        <h2 className="mb-0">HR Assistant</h2>
        <small className="text-muted">
          Ask questions about company HR policies. Answers are generated only from HR-approved
          policy documents — for official decisions, always contact HR directly.
        </small>
      </div>

      <div className="row g-3" style={{ minHeight: '60vh' }}>
        <div className="col-md-3">
          <div className="card h-100">
            <div className="card-header d-flex justify-content-between align-items-center">
              <h6 className="mb-0">Conversations</h6>
              <button className="btn btn-sm btn-primary" onClick={handleNewConversation}>
                + New
              </button>
            </div>
            <div className="list-group list-group-flush" style={{ maxHeight: '55vh', overflowY: 'auto' }}>
              {loadingConversations ? (
                <div className="d-flex justify-content-center py-4">
                  <div className="spinner-border spinner-border-sm text-primary" role="status">
                    <span className="visually-hidden">Loading…</span>
                  </div>
                </div>
              ) : conversations.length === 0 ? (
                <div className="text-muted small p-3">No conversations yet.</div>
              ) : (
                conversations.map(c => (
                  <button
                    key={c.id}
                    type="button"
                    className={
                      'list-group-item list-group-item-action d-flex justify-content-between align-items-center' +
                      (c.id === selectedConversationId ? ' active' : '')
                    }
                    onClick={() => handleSelectConversation(c.id)}
                  >
                    <span className="small">
                      Conversation #{c.id}
                      <br />
                      <span className="text-muted">{formatDateTime(c.updatedAt)}</span>
                    </span>
                    <span
                      role="button"
                      className="btn-close btn-close-white ms-2"
                      style={{ filter: c.id === selectedConversationId ? 'invert(1) grayscale(1)' : 'none' }}
                      aria-label="Delete conversation"
                      onClick={e => handleDeleteConversation(c.id, e)}
                    />
                  </button>
                ))
              )}
            </div>
          </div>
        </div>

        <div className="col-md-9">
          <div className="card h-100 d-flex flex-column">
            <div className="card-body d-flex flex-column" style={{ overflowY: 'auto', maxHeight: '55vh' }}>
              {error && (
                <div className="alert alert-danger alert-dismissible" role="alert">
                  {error}
                  <button type="button" className="btn-close" aria-label="Close" onClick={() => setError(null)} />
                </div>
              )}

              {aiDisabled && (
                <div className="alert alert-warning" role="alert">
                  The HR Assistant is currently unavailable. Please try again later, or contact HR
                  directly for help.
                </div>
              )}

              {loadingMessages ? (
                <div className="d-flex justify-content-center py-5">
                  <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading…</span>
                  </div>
                </div>
              ) : messages.length === 0 ? (
                <div className="text-center text-muted py-5">
                  <p className="fs-5 mb-1">Ask the HR Assistant a question</p>
                  <p className="mb-0 small">e.g. &quot;How many vacation days can I carry over?&quot;</p>
                </div>
              ) : (
                <>
                  {messages.map((m, i) => (
                    <div
                      key={i}
                      className={'d-flex mb-3 ' + (m.role === 'USER' ? 'justify-content-end' : 'justify-content-start')}
                    >
                      <div
                        className={
                          'p-3 rounded-3 ' +
                          (m.role === 'USER' ? 'bg-primary text-white' : 'bg-light border')
                        }
                        style={{ maxWidth: '75%', whiteSpace: 'pre-wrap' }}
                      >
                        <div>{m.content}</div>
                        {m.role === 'ASSISTANT' && (
                          <>
                            {m.grounded === false && (
                              <div className="small fst-italic text-muted mt-2">
                                This answer is not grounded in a specific policy document — please
                                confirm with HR.
                              </div>
                            )}
                            <SourceList sources={m.sources} />
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>

            <form className="card-footer d-flex gap-2" onSubmit={handleSubmit}>
              <input
                type="text"
                className="form-control"
                placeholder="Ask about an HR policy…"
                value={inputValue}
                onChange={e => setInputValue(e.target.value)}
                maxLength={2000}
                disabled={sending}
              />
              <button type="submit" className="btn btn-primary" disabled={sending || !inputValue.trim()}>
                {sending ? (
                  <><span className="spinner-border spinner-border-sm me-1" />Sending…</>
                ) : (
                  'Send'
                )}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

export default HrAssistantPage
