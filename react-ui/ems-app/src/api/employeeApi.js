import axiosClient from './axiosClient.js'

const BASE = '/employees'

/** params: { keyword, departmentId, designationId, status, page, size, sortBy, sortDir } - all optional. */
export const listEmployees = (params = {}) => axiosClient.get(BASE, { params })
export const getEmployee = (id) => axiosClient.get(`${BASE}/${id}`)
export const createEmployee = (payload) => axiosClient.post(BASE, payload)
export const updateEmployee = (id, payload) => axiosClient.put(`${BASE}/${id}`, payload)
export const deleteEmployee = (id) => axiosClient.delete(`${BASE}/${id}`)
