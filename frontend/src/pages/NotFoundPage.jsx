import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <div className="text-center py-5">
      <h2 className="text-muted mb-3">404 — Page Not Found</h2>
      <p className="text-muted mb-4">
        The page you are looking for does not exist.
      </p>
      <Link to="/employees" className="btn btn-primary">
        Back to Employees
      </Link>
    </div>
  )
}

export default NotFoundPage
