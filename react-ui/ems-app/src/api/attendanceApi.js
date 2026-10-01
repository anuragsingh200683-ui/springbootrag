import axiosClient from './axiosClient.js'

const BASE = '/attendance'

export const checkIn = (employeeId) => axiosClient.post(`${BASE}/check-in`, { employeeId })
export const checkOut = (employeeId) => axiosClient.post(`${BASE}/check-out`, { employeeId })
/** params: { employeeId, from, to, page, size, sortBy, sortDir } - all optional. */
export const getAttendanceHistory = (params = {}) => axiosClient.get(BASE, { params })
