import httpClient from './httpClient.js'

const hrAssistantService = {
  chat: (message, conversationId) =>
    httpClient.post('/ai/hr-assistant/chat', { message, conversationId: conversationId ?? null }),

  getConversations: (page = 0, size = 20) =>
    httpClient.get('/ai/hr-assistant/conversations', { params: { page, size } }),

  getConversation: id =>
    httpClient.get(`/ai/hr-assistant/conversations/${id}`),

  deleteConversation: id =>
    httpClient.delete(`/ai/hr-assistant/conversations/${id}`),
}

export default hrAssistantService
