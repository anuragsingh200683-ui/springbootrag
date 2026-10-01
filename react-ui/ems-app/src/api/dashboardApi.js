import axiosClient from './axiosClient.js'

export const getDashboardSummary = () => axiosClient.get('/dashboard/summary')
