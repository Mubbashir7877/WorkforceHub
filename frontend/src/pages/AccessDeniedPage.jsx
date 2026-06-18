import { Link } from 'react-router-dom'

function AccessDeniedPage() {
  return (
    <div className="text-center py-5">
      <h2 className="text-danger mb-3">403 — Access Denied</h2>
      <p className="text-muted mb-4">You do not have permission to view this page.</p>
      <Link to="/profile" className="btn btn-primary">
        Back to My Account
      </Link>
    </div>
  )
}

export default AccessDeniedPage
