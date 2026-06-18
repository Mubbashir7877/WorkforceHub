import httpClient from './httpClient.js'

const employeeService = {
  getAll() {
    return httpClient.get('/employees')
  },

  getById(id) {
    return httpClient.get(`/employees/${id}`)
  },

  getOwn() {
    return httpClient.get('/employees/me')
  },

  updateOwn(data) {
    return httpClient.put('/employees/me', data)
  },

  create(data) {
    return httpClient.post('/employees', data)
  },

  update(id, data) {
    return httpClient.put(`/employees/${id}`, data)
  },

  delete(id) {
    return httpClient.delete(`/employees/${id}`)
  },
}

export default employeeService
