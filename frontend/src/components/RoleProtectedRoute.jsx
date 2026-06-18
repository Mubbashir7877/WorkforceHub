import { Navigate, Outlet } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'

function RoleProtectedRoute({ roles }) {
  const { hasAnyRole } = useAuth()

  if (!hasAnyRole(roles)) {
    return <Navigate to="/access-denied" replace />
  }

  return <Outlet />
}

export default RoleProtectedRoute
