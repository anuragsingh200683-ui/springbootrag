import { useEffect, useState } from 'react'
import { Box, Grid, Paper, Typography, CircularProgress, Alert, LinearProgress, Stack } from '@mui/material'
import PeopleIcon from '@mui/icons-material/People'
import ApartmentIcon from '@mui/icons-material/Apartment'
import BadgeIcon from '@mui/icons-material/Badge'
import EventAvailableIcon from '@mui/icons-material/EventAvailable'
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty'
import { getDashboardSummary } from '../../api/dashboardApi.js'
import SummaryCard from './SummaryCard.jsx'

export default function DashboardPage() {
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    getDashboardSummary().then(setSummary).catch((err) => setError(err.message))
  }, [])

  if (error) return <Alert severity="error">{error}</Alert>
  if (!summary) return <CircularProgress />

  const maxDeptCount = Math.max(1, ...summary.employeesByDepartment.map((d) => d.employeeCount))

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Dashboard</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>As of {summary.asOf}</Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard label="Active Employees" value={summary.activeEmployees} icon={<PeopleIcon fontSize="large" />} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard label="Total Employees" value={summary.totalEmployees} icon={<PeopleIcon fontSize="large" />} color="text.secondary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard label="Departments" value={summary.totalDepartments} icon={<ApartmentIcon fontSize="large" />} color="secondary.main" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <SummaryCard label="Designations" value={summary.totalDesignations} icon={<BadgeIcon fontSize="large" />} color="secondary.main" />
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <SummaryCard label="Checked In Today" value={summary.checkedInToday} icon={<EventAvailableIcon fontSize="large" />} color="info.main" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SummaryCard label="Checked Out Today" value={summary.checkedOutToday} icon={<EventAvailableIcon fontSize="large" />} color="success.main" />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SummaryCard label="Not Checked In" value={summary.notCheckedInToday} icon={<HourglassEmptyIcon fontSize="large" />} color="warning.main" />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>Employees by Department</Typography>
            {summary.employeesByDepartment.length === 0 ? (
              <Typography color="text.secondary">No data yet.</Typography>
            ) : (
              <Stack spacing={1.5}>
                {summary.employeesByDepartment.map((d) => (
                  <Box key={d.departmentName}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">{d.departmentName}</Typography>
                      <Typography variant="body2" color="text.secondary">{d.employeeCount}</Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={(d.employeeCount / maxDeptCount) * 100} sx={{ height: 8, borderRadius: 4 }} />
                  </Box>
                ))}
              </Stack>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>Leave Applications</Typography>
            <Stack spacing={1.5}>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2">Pending</Typography>
                <Typography variant="body2" fontWeight={600} color="warning.main">{summary.pendingLeaves}</Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2">Approved</Typography>
                <Typography variant="body2" fontWeight={600} color="success.main">{summary.approvedLeaves}</Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2">Rejected</Typography>
                <Typography variant="body2" fontWeight={600} color="error.main">{summary.rejectedLeaves}</Typography>
              </Stack>
            </Stack>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}
