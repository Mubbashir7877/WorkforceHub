import httpClient from './httpClient.js'

const timeClockService = {
  getStatus: () =>
    httpClient.get('/time-clock/status'),

  clockIn: () =>
    httpClient.post('/time-clock/clock-in'),

  clockOut: () =>
    httpClient.post('/time-clock/clock-out'),

  getMySessions: (page = 0, size = 10) =>
    httpClient.get('/time-clock/my-sessions', { params: { page, size } }),

  getEvents: (params = {}) => {
    const query = { page: 0, size: 20, ...params }
    Object.keys(query).forEach(k => {
      if (query[k] === null || query[k] === undefined || query[k] === '') {
        delete query[k]
      }
    })
    return httpClient.get('/time-clock/events', { params: query })
  },
}

export default timeClockService
