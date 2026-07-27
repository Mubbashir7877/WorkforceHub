import httpClient from './httpClient.js'

const hrPolicyService = {
  list: (params = {}) => {
    const query = { page: 0, size: 20, ...params }
    Object.keys(query).forEach(k => {
      if (query[k] === null || query[k] === undefined || query[k] === '') {
        delete query[k]
      }
    })
    return httpClient.get('/hr/policies', { params: query })
  },

  getById: id => httpClient.get(`/hr/policies/${id}`),

  create: payload => httpClient.post('/hr/policies', payload),

  update: (id, payload) => httpClient.put(`/hr/policies/${id}`, payload),

  upload: (id, file) => {
    const formData = new FormData()
    formData.append('file', file)
    return httpClient.post(`/hr/policies/${id}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  process: id => httpClient.post(`/hr/policies/${id}/process`),

  reviewConflicts: id => httpClient.post(`/hr/policies/${id}/review-conflicts`),

  activate: id => httpClient.post(`/hr/policies/${id}/activate`),

  deactivate: id => httpClient.post(`/hr/policies/${id}/deactivate`),

  download: async (id, filename) => {
    const res = await httpClient.get(`/hr/policies/${id}/download`, { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename || `policy-document-${id}`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  },
}

export default hrPolicyService
