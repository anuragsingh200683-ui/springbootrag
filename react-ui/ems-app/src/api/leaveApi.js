import axiosClient from './axiosClient.js'

const BASE = '/leaves'

export const applyLeave = (payload) => axiosClient.post(BASE, payload)
export const approveLeave = (id, payload) => axiosClient.put(`${BASE}/${id}/approve`, payload)
export const rejectLeave = (id, payload) => axiosClient.put(`${BASE}/${id}/reject`, payload)
/** params: { employeeId, status, from, to, page, size, sortBy, sortDir } - all optional. */
export const getLeaveHistory = (params = {}) => axiosClient.get(BASE, { params })
