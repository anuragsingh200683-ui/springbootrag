import axiosClient from './axiosClient.js'

const BASE = '/designations'

export const listDesignations = () => axiosClient.get(BASE)
export const getDesignation = (id) => axiosClient.get(`${BASE}/${id}`)
export const createDesignation = (payload) => axiosClient.post(BASE, payload)
export const updateDesignation = (id, payload) => axiosClient.put(`${BASE}/${id}`, payload)
export const deleteDesignation = (id) => axiosClient.delete(`${BASE}/${id}`)
