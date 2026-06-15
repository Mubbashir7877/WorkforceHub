import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
})

const employeeService = {
  getAll() {
    return apiClient.get('/employees')
  },

  getById(id) {
    return apiClient.get(`/employees/${id}`)
  },

  create(data) {
    return apiClient.post('/employees', data)
  },

  update(id, data) {
    return apiClient.put(`/employees/${id}`, data)
  },

  delete(id) {
    return apiClient.delete(`/employees/${id}`)
  },
}

export default employeeService
