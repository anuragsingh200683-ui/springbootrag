import { useCallback, useEffect, useState } from 'react'
import {
  Box, Button, Paper, Snackbar, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, CircularProgress, TextField,
  Stack, TablePagination,
} from '@mui/material'
import LoginIcon from '@mui/icons-material/Login'
import LogoutIcon from '@mui/icons-material/Logout'
import { checkIn, checkOut, getAttendanceHistory } from '../../api/attendanceApi.js'
import EmployeeAutocomplete from '../common/EmployeeAutocomplete.jsx'
import StatusChip from '../common/StatusChip.jsx'

function formatTime(iso) {
  if (!iso) return '-'
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export default function AttendancePage() {
  const [selectedEmployee, setSelectedEmployee] = useState(null)
  const [actionLoading, setActionLoading] = useState(false)
  const [snackbar, setSnackbar] = useState(null)

  const [filterEmployee, setFilterEmployee] = useState(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  const load = useCallback(() => {
    setLoading(true)
    getAttendanceHistory({
      employeeId: filterEmployee?.id || undefined,
      from: from || undefined,
      to: to || undefined,
      page: pageIndex,
      size: pageSize,
      sortBy: 'attendanceDate',
      sortDir: 'desc',
    })
      .then(setPage)
      .catch((err) => setSnackbar({ severity: 'error', message: err.message }))
      .finally(() => setLoading(false))
  }, [filterEmployee, from, to, pageIndex, pageSize])

  useEffect(() => { load() }, [load])

  const handleCheckIn = async () => {
    if (!selectedEmployee) return
    setActionLoading(true)
    try {
      await checkIn(selectedEmployee.id)
      setSnackbar({ severity: 'success', message: `${selectedEmployee.firstName} checked in` })
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    } finally {
      setActionLoading(false)
    }
  }

  const handleCheckOut = async () => {
    if (!selectedEmployee) return
    setActionLoading(true)
    try {
      await checkOut(selectedEmployee.id)
      setSnackbar({ severity: 'success', message: `${selectedEmployee.firstName} checked out` })
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Attendance</Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle1" sx={{ mb: 2 }}>Check In / Check Out</Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <Box sx={{ minWidth: 320 }}>
            <EmployeeAutocomplete value={selectedEmployee} onChange={setSelectedEmployee} />
          </Box>
          <Button variant="contained" startIcon={<LoginIcon />} disabled={!selectedEmployee || actionLoading}
            onClick={handleCheckIn}>
            Check In
          </Button>
          <Button variant="outlined" startIcon={<LogoutIcon />} disabled={!selectedEmployee || actionLoading}
            onClick={handleCheckOut}>
            Check Out
          </Button>
        </Stack>
      </Paper>

      <Typography variant="subtitle1" sx={{ mb: 1 }}>History</Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <Box sx={{ minWidth: 260 }}>
          <EmployeeAutocomplete value={filterEmployee} onChange={setFilterEmployee} label="Filter by employee" />
        </Box>
        <TextField label="From" type="date" value={from} onChange={(e) => { setPageIndex(0); setFrom(e.target.value) }}
          InputLabelProps={{ shrink: true }} size="small" />
        <TextField label="To" type="date" value={to} onChange={(e) => { setPageIndex(0); setTo(e.target.value) }}
          InputLabelProps={{ shrink: true }} size="small" />
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Employee</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Check In</TableCell>
                <TableCell>Check Out</TableCell>
                <TableCell>Worked (min)</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={6} align="center"><CircularProgress size={28} sx={{ my: 2 }} /></TableCell></TableRow>
              ) : page.content.length === 0 ? (
                <TableRow><TableCell colSpan={6} align="center">No attendance records found.</TableCell></TableRow>
              ) : (
                page.content.map((rec) => (
                  <TableRow key={rec.id} hover>
                    <TableCell>{rec.employeeName}</TableCell>
                    <TableCell>{rec.attendanceDate}</TableCell>
                    <TableCell>{formatTime(rec.checkInTime)}</TableCell>
                    <TableCell>{formatTime(rec.checkOutTime)}</TableCell>
                    <TableCell>{rec.workingMinutes ?? '-'}</TableCell>
                    <TableCell><StatusChip status={rec.status} /></TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={page.totalElements}
          page={pageIndex}
          onPageChange={(_, newPage) => setPageIndex(newPage)}
          rowsPerPage={pageSize}
          onRowsPerPageChange={(e) => { setPageSize(parseInt(e.target.value, 10)); setPageIndex(0) }}
          rowsPerPageOptions={[10, 25, 50]}
        />
      </Paper>

      <Snackbar open={!!snackbar} autoHideDuration={4000} onClose={() => setSnackbar(null)}>
        {snackbar && <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)}>{snackbar.message}</Alert>}
      </Snackbar>
    </Box>
  )
}
