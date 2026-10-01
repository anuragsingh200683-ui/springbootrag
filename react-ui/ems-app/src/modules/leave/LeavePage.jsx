import { useCallback, useEffect, useState } from 'react'
import {
  Box, Button, IconButton, Paper, Snackbar, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, CircularProgress, TextField,
  MenuItem, Stack, TablePagination, Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import { applyLeave, approveLeave, rejectLeave, getLeaveHistory } from '../../api/leaveApi.js'
import EmployeeAutocomplete from '../common/EmployeeAutocomplete.jsx'
import StatusChip from '../common/StatusChip.jsx'
import LeaveApplyDialog from './LeaveApplyDialog.jsx'
import LeaveDecisionDialog from './LeaveDecisionDialog.jsx'
import { isAdmin } from '../../auth/keycloak.js'

export default function LeavePage() {
  const [filterEmployee, setFilterEmployee] = useState(null)
  const [status, setStatus] = useState('')
  const [page, setPage] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize, setPageSize] = useState(10)

  const [applyOpen, setApplyOpen] = useState(false)
  const [decision, setDecision] = useState(null) // { leave, action }
  const [snackbar, setSnackbar] = useState(null)
  // Approve/reject are ADMIN-only on the API; anyone signed in can apply.
  const admin = isAdmin()

  const load = useCallback(() => {
    setLoading(true)
    getLeaveHistory({
      employeeId: filterEmployee?.id || undefined,
      status: status || undefined,
      page: pageIndex,
      size: pageSize,
      sortBy: 'appliedAt',
      sortDir: 'desc',
    })
      .then(setPage)
      .catch((err) => setSnackbar({ severity: 'error', message: err.message }))
      .finally(() => setLoading(false))
  }, [filterEmployee, status, pageIndex, pageSize])

  useEffect(() => { load() }, [load])

  const handleApply = async (form) => {
    try {
      await applyLeave(form)
      setSnackbar({ severity: 'success', message: 'Leave application submitted' })
      setApplyOpen(false)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  const handleDecision = async (form) => {
    const { leave, action } = decision
    try {
      if (action === 'approve') await approveLeave(leave.id, form)
      else await rejectLeave(leave.id, form)
      setSnackbar({ severity: 'success', message: `Leave ${action === 'approve' ? 'approved' : 'rejected'}` })
      setDecision(null)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Leave</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setApplyOpen(true)}>Apply Leave</Button>
      </Box>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <Box sx={{ minWidth: 260 }}>
          <EmployeeAutocomplete value={filterEmployee} onChange={setFilterEmployee} label="Filter by employee" />
        </Box>
        <TextField select label="Status" size="small" sx={{ minWidth: 160 }}
          value={status} onChange={(e) => { setPageIndex(0); setStatus(e.target.value) }}>
          <MenuItem value="">All</MenuItem>
          <MenuItem value="PENDING">Pending</MenuItem>
          <MenuItem value="APPROVED">Approved</MenuItem>
          <MenuItem value="REJECTED">Rejected</MenuItem>
        </TextField>
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Employee</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Dates</TableCell>
                <TableCell>Days</TableCell>
                <TableCell>Reason</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={7} align="center"><CircularProgress size={28} sx={{ my: 2 }} /></TableCell></TableRow>
              ) : page.content.length === 0 ? (
                <TableRow><TableCell colSpan={7} align="center">No leave applications found.</TableCell></TableRow>
              ) : (
                page.content.map((leave) => (
                  <TableRow key={leave.id} hover>
                    <TableCell>{leave.employeeName}</TableCell>
                    <TableCell>{leave.leaveType}</TableCell>
                    <TableCell>{leave.startDate} → {leave.endDate}</TableCell>
                    <TableCell>{leave.numberOfDays}</TableCell>
                    <TableCell sx={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {leave.reason}
                    </TableCell>
                    <TableCell><StatusChip status={leave.status} /></TableCell>
                    <TableCell align="right">
                      {leave.status === 'PENDING' ? (admin ? (
                        <>
                          <Tooltip title="Approve">
                            <IconButton size="small" color="success" onClick={() => setDecision({ leave, action: 'approve' })}>
                              <CheckIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Reject">
                            <IconButton size="small" color="error" onClick={() => setDecision({ leave, action: 'reject' })}>
                              <CloseIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </>
                      ) : (
                        <Typography variant="caption" color="text.secondary">Pending approval</Typography>
                      )) : (
                        <Typography variant="caption" color="text.secondary">
                          {leave.decidedBy ? `by ${leave.decidedBy}` : '-'}
                        </Typography>
                      )}
                    </TableCell>
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

      <LeaveApplyDialog open={applyOpen} onClose={() => setApplyOpen(false)} onSubmit={handleApply} />

      <LeaveDecisionDialog
        open={!!decision}
        action={decision?.action}
        onClose={() => setDecision(null)}
        onSubmit={handleDecision}
      />

      <Snackbar open={!!snackbar} autoHideDuration={4000} onClose={() => setSnackbar(null)}>
        {snackbar && <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)}>{snackbar.message}</Alert>}
      </Snackbar>
    </Box>
  )
}
