import { Link, NavLink, useNavigate } from 'react-router-dom'
import useAuth from '../hooks/useAuth.js'
import { ROLES } from '../constants/roles.js'

const navLinkClass = ({ isActive }) => 'nav-link' + (isActive ? ' active' : '')

function Header() {
  const { user, isAuthenticated, logout, hasAnyRole, hasRole } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div className="container">
        <Link className="navbar-brand fw-bold" to="/">
          Employee Management System
        </Link>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#mainNav"
          aria-controls="mainNav"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="mainNav">
          <ul className="navbar-nav ms-auto align-items-lg-center">
            {isAuthenticated && (
              <li className="nav-item">
                <NavLink className={navLinkClass} to="/time-clock">
                  My Time Clock
                </NavLink>
              </li>
            )}
            {isAuthenticated && (
              <li className="nav-item">
                <NavLink className={navLinkClass} to="/ai/hr-assistant">
                  HR Assistant
                </NavLink>
              </li>
            )}
            {isAuthenticated && hasAnyRole([ROLES.MANAGER, ROLES.HR_ADMIN, ROLES.SYSTEM_ADMIN]) && (
              <>
                <li className="nav-item">
                  <NavLink className={navLinkClass} to="/employees">
                    Employees
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink className={navLinkClass} to="/time-clock/events">
                    Time Clock Records
                  </NavLink>
                </li>
              </>
            )}
            {isAuthenticated && hasAnyRole([ROLES.HR_ADMIN, ROLES.SYSTEM_ADMIN]) && (
              <li className="nav-item">
                <NavLink className={navLinkClass} to="/hr/policies">
                  Policy Documents
                </NavLink>
              </li>
            )}
            {isAuthenticated && hasRole(ROLES.SYSTEM_ADMIN) && (
              <>
                <li className="nav-item">
                  <NavLink className={navLinkClass} to="/admin/users">
                    User Administration
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink className={navLinkClass} to="/system/activity">
                    System Activity
                  </NavLink>
                </li>
              </>
            )}
            {isAuthenticated ? (
              <>
                <li className="nav-item">
                  <NavLink className={navLinkClass} to="/profile">
                    {user.email}
                  </NavLink>
                </li>
                <li className="nav-item">
                  <button className="btn btn-outline-light btn-sm ms-lg-2" onClick={handleLogout}>
                    Logout
                  </button>
                </li>
              </>
            ) : (
              <li className="nav-item">
                <NavLink className={navLinkClass} to="/login">
                  Login
                </NavLink>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  )
}

export default Header
