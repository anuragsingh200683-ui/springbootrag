import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Box, Button, IconButton, Paper, Snackbar, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, CircularProgress, TextField,
  MenuItem, Stack, TablePagination, TableSortLabel, InputAdornment,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import SearchIcon from '@mui/icons-material/Search'
import { listEmployees, createEmployee, updateEmployee, deleteEmployee } from '../../api/employeeApi.js'
import { listDepartments } from '../../api/departmentApi.js'
import { listDesignations } from '../../api/designationApi.js'
import EmployeeFormDialog from './EmployeeFormDialog.jsx'
import ConfirmDialog from '../common/ConfirmDialog.jsx'
import StatusChip from '../common/StatusChip.jsx'
import { isAdmin } from '../../auth/keycloak.js'

const SORTABLE_COLUMNS = [
  { key: 'firstName', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'dateOfJoining', label: 'Joined' },
  { key: 'salary', label: 'Salary' },
  { key: 'status', label: 'Status' },
]

export default function EmployeePage() {
  // Allows the AI Assistant's natural-language search to deep-link here with
  // pre-filled filters, e.g. /employees?keyword=John&departmentId=1&status=ACTIVE.
  const [searchParams] = useSearchParams()

  const [page, setPage] = useState({ content: [], page: 0, size: 10, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const [departmentId, setDepartmentId] = useState(searchParams.get('departmentId') || '')
  const [designationId, setDesignationId] = useState(searchParams.get('designationId') || '')
  const [status, setStatus] = useState(searchParams.get('status') || '')
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [sortBy, setSortBy] = useState('firstName')
  const [sortDir, setSortDir] = useState('asc')

  const [departments, setDepartments] = useState([])
  const [designations, setDesignations] = useState([])

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [snackbar, setSnackbar] = useState(null)
  // Create/edit/delete are ADMIN-only on the API; hide them from everyone else.
  const admin = isAdmin()

  useEffect(() => {
    listDepartments().then(setDepartments).catch(() => setDepartments([]))
    listDesignations().then(setDesignations).catch(() => setDesignations([]))
  }, [])

  const load = useCallback(() => {
    setLoading(true)
    listEmployees({
      keyword: keyword || undefined,
      departmentId: departmentId || undefined,
      designationId: designationId || undefined,
      status: status || undefined,
      page: pageIndex,
      size: pageSize,
      sortBy,
      sortDir,
    })
      .then(setPage)
      .catch((err) => setSnackbar({ severity: 'error', message: err.message }))
      .finally(() => setLoading(false))
  }, [keyword, departmentId, designationId, status, pageIndex, pageSize, sortBy, sortDir])

  useEffect(() => { load() }, [load])

  const handleSort = (column) => {
    if (sortBy === column) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortBy(column)
      setSortDir('asc')
    }
  }

  const handleAdd = () => { setEditing(null); setFormOpen(true) }
  const handleEdit = (emp) => { setEditing(emp); setFormOpen(true) }

  const handleSubmit = async (form) => {
    try {
      if (editing) {
        await updateEmployee(editing.id, form)
        setSnackbar({ severity: 'success', message: 'Employee updated' })
      } else {
        await createEmployee(form)
        setSnackbar({ severity: 'success', message: 'Employee created' })
      }
      setFormOpen(false)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  const handleDelete = async () => {
    try {
      await deleteEmployee(deleteTarget.id)
      setSnackbar({ severity: 'success', message: 'Employee deleted' })
      setDeleteTarget(null)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Employees</Typography>
        {admin && <Button variant="contained" startIcon={<AddIcon />} onClick={handleAdd}>Add Employee</Button>}
      </Box>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <TextField
          placeholder="Search name or email"
          value={keyword}
          onChange={(e) => { setPageIndex(0); setKeyword(e.target.value) }}
          size="small"
          sx={{ minWidth: 240 }}
          InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
        />
        <TextField select label="Department" size="small" sx={{ minWidth: 180 }}
          value={departmentId} onChange={(e) => { setPageIndex(0); setDepartmentId(e.target.value) }}>
          <MenuItem value="">All</MenuItem>
          {departments.map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
        </TextField>
        <TextField select label="Designation" size="small" sx={{ minWidth: 180 }}
          value={designationId} onChange={(e) => { setPageIndex(0); setDesignationId(e.target.value) }}>
          <MenuItem value="">All</MenuItem>
          {designations.map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
        </TextField>
        <TextField select label="Status" size="small" sx={{ minWidth: 140 }}
          value={status} onChange={(e) => { setPageIndex(0); setStatus(e.target.value) }}>
          <MenuItem value="">All</MenuItem>
          <MenuItem value="ACTIVE">Active</MenuItem>
          <MenuItem value="INACTIVE">Inactive</MenuItem>
        </TextField>
      </Stack>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {SORTABLE_COLUMNS.map((col) => (
                  <TableCell key={col.key}>
                    <TableSortLabel
                      active={sortBy === col.key}
                      direction={sortBy === col.key ? sortDir : 'asc'}
                      onClick={() => handleSort(col.key)}
                    >
                      {col.label}
                    </TableSortLabel>
                  </TableCell>
                ))}
                <TableCell>Department</TableCell>
                <TableCell>Designation</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow><TableCell colSpan={8} align="center"><CircularProgress size={28} sx={{ my: 2 }} /></TableCell></TableRow>
              ) : page.content.length === 0 ? (
                <TableRow><TableCell colSpan={8} align="center">No employees found.</TableCell></TableRow>
              ) : (
                page.content.map((emp) => (
                  <TableRow key={emp.id} hover>
                    <TableCell>
                      <Typography variant="body2">{emp.firstName} {emp.lastName}</Typography>
                      <Typography variant="caption" color="text.secondary">{emp.employeeCode}</Typography>
                    </TableCell>
                    <TableCell>{emp.email}</TableCell>
                    <TableCell>{emp.dateOfJoining}</TableCell>
                    <TableCell>{emp.salary}</TableCell>
                    <TableCell><StatusChip status={emp.status} /></TableCell>
                    <TableCell>{emp.department?.name}</TableCell>
                    <TableCell>{emp.designation?.name}</TableCell>
                    <TableCell align="right">
                      {admin && (
                        <>
                          <IconButton size="small" onClick={() => handleEdit(emp)}><EditIcon fontSize="small" /></IconButton>
                          <IconButton size="small" onClick={() => setDeleteTarget(emp)}><DeleteIcon fontSize="small" /></IconButton>
                        </>
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

      <EmployeeFormDialog
        open={formOpen}
        initialValue={editing}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete employee"
        message={`Delete "${deleteTarget?.firstName} ${deleteTarget?.lastName}"? This cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />

      <Snackbar open={!!snackbar} autoHideDuration={4000} onClose={() => setSnackbar(null)}>
        {snackbar && <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)}>{snackbar.message}</Alert>}
      </Snackbar>
    </Box>
  )
}
