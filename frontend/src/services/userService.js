import httpClient from './httpClient.js'

const userService = {
  list() {
    return httpClient.get('/admin/users')
  },

  getById(id) {
    return httpClient.get(`/admin/users/${id}`)
  },

  create(data) {
    return httpClient.post('/admin/users', data)
  },

  enable(id) {
    return httpClient.post(`/admin/users/${id}/enable`)
  },

  disable(id) {
    return httpClient.post(`/admin/users/${id}/disable`)
  },

  updateRoles(id, roles) {
    return httpClient.put(`/admin/users/${id}/roles`, { roles })
  },

  linkEmployee(id, employeeId) {
    return httpClient.put(`/admin/users/${id}/employee`, { employeeId })
  },

  revokeTokens(id) {
    return httpClient.post(`/admin/users/${id}/revoke-tokens`)
  },
}

export default userService
