import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from '../layout/MainLayout.jsx'
import DashboardPage from '../modules/dashboard/DashboardPage.jsx'
import EmployeePage from '../modules/employee/EmployeePage.jsx'
import DepartmentPage from '../modules/department/DepartmentPage.jsx'
import DesignationPage from '../modules/designation/DesignationPage.jsx'
import AttendancePage from '../modules/attendance/AttendancePage.jsx'
import LeavePage from '../modules/leave/LeavePage.jsx'
import AiAssistantPage from '../modules/aiassistant/AiAssistantPage.jsx'

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/employees" element={<EmployeePage />} />
        <Route path="/departments" element={<DepartmentPage />} />
        <Route path="/designations" element={<DesignationPage />} />
        <Route path="/attendance" element={<AttendancePage />} />
        <Route path="/leave" element={<LeavePage />} />
        <Route path="/ai-assistant" element={<AiAssistantPage />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Route>
    </Routes>
  )
}
