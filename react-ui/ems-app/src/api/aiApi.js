import axiosClient from './axiosClient.js'

const BASE = '/ai'

export const getEmployeeAiSummary = (employeeId) => axiosClient.get(`${BASE}/employees/${employeeId}/summary`)
export const generateEmployeeProfile = (employeeId, payload) => axiosClient.post(`${BASE}/employees/${employeeId}/profile`, payload)
export const chatWithAssistant = (payload) => axiosClient.post(`${BASE}/chat`, payload)
export const naturalLanguageSearch = (query) => axiosClient.post(`${BASE}/search`, { query })
