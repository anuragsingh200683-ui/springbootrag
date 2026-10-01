import axiosClient from './axiosClient.js'

const BASE = '/departments'

export const listDepartments = () => axiosClient.get(BASE)
export const getDepartment = (id) => axiosClient.get(`${BASE}/${id}`)
export const createDepartment = (payload) => axiosClient.post(BASE, payload)
export const updateDepartment = (id, payload) => axiosClient.put(`${BASE}/${id}`, payload)
export const deleteDepartment = (id) => axiosClient.delete(`${BASE}/${id}`)
