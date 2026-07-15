import { Routes, Route, Navigate } from 'react-router-dom'
import Header from './components/Header.jsx'
import Footer from './components/Footer.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import RoleProtectedRoute from './components/RoleProtectedRoute.jsx'
import LoginPage from './pages/LoginPage.jsx'
import AccessDeniedPage from './pages/AccessDeniedPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import UserAdminPage from './pages/UserAdminPage.jsx'
import SystemActivityPage from './pages/SystemActivityPage.jsx'
import EmployeeListPage from './pages/EmployeeListPage.jsx'
import EmployeeFormPage from './pages/EmployeeFormPage.jsx'
import MyTimeClockPage from './pages/MyTimeClockPage.jsx'
import TimeClockEventsPage from './pages/TimeClockEventsPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import { ROLES } from './constants/roles.js'

function App() {
  return (
    <div className="d-flex flex-column min-vh-100">
      <Header />
      <main className="flex-grow-1 container py-4">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/access-denied" element={<AccessDeniedPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Navigate to="/profile" replace />} />
            <Route path="/profile" element={<ProfilePage />} />

            <Route path="/time-clock" element={<MyTimeClockPage />} />

            <Route element={<RoleProtectedRoute roles={[ROLES.MANAGER, ROLES.HR_ADMIN, ROLES.SYSTEM_ADMIN]} />}>
              <Route path="/employees" element={<EmployeeListPage />} />
              <Route path="/time-clock/events" element={<TimeClockEventsPage />} />
            </Route>

            <Route element={<RoleProtectedRoute roles={[ROLES.HR_ADMIN, ROLES.SYSTEM_ADMIN]} />}>
              <Route path="/employees/new" element={<EmployeeFormPage />} />
              <Route path="/employees/:id/edit" element={<EmployeeFormPage />} />
            </Route>

            <Route element={<RoleProtectedRoute roles={[ROLES.SYSTEM_ADMIN]} />}>
              <Route path="/admin/users" element={<UserAdminPage />} />
              <Route path="/system/activity" element={<SystemActivityPage />} />
            </Route>
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  )
}

export default App
