import httpClient from './httpClient.js'

const activityLogService = {
  getLogs: (page = 0, size = 20, eventType = null) => {
    const params = { page, size }
    if (eventType) params.eventType = eventType
    return httpClient.get('/system/activity-logs', { params })
  },
}

export default activityLogService
