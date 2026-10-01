import { useCallback, useEffect, useState } from 'react'
import {
  Box, Button, IconButton, Paper, Snackbar, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, CircularProgress,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { listDepartments, createDepartment, updateDepartment, deleteDepartment } from '../../api/departmentApi.js'
import DepartmentFormDialog from './DepartmentFormDialog.jsx'
import ConfirmDialog from '../common/ConfirmDialog.jsx'

export default function DepartmentPage() {
  const [departments, setDepartments] = useState([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [snackbar, setSnackbar] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    listDepartments()
      .then(setDepartments)
      .catch((err) => setSnackbar({ severity: 'error', message: err.message }))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const handleAdd = () => { setEditing(null); setFormOpen(true) }
  const handleEdit = (dept) => { setEditing(dept); setFormOpen(true) }

  const handleSubmit = async (form) => {
    try {
      if (editing) {
        await updateDepartment(editing.id, form)
        setSnackbar({ severity: 'success', message: 'Department updated' })
      } else {
        await createDepartment(form)
        setSnackbar({ severity: 'success', message: 'Department created' })
      }
      setFormOpen(false)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  const handleDelete = async () => {
    try {
      await deleteDepartment(deleteTarget.id)
      setSnackbar({ severity: 'success', message: 'Department deleted' })
      setDeleteTarget(null)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Departments</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleAdd}>Add Department</Button>
      </Box>

      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Description</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={3} align="center">
                    <CircularProgress size={28} sx={{ my: 2 }} />
                  </TableCell>
                </TableRow>
              ) : departments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center">No departments yet.</TableCell>
                </TableRow>
              ) : (
                departments.map((dept) => (
                  <TableRow key={dept.id} hover>
                    <TableCell>{dept.name}</TableCell>
                    <TableCell>{dept.description}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" onClick={() => handleEdit(dept)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" onClick={() => setDeleteTarget(dept)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <DepartmentFormDialog
        open={formOpen}
        initialValue={editing}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete department"
        message={`Delete "${deleteTarget?.name}"? This cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />

      <Snackbar open={!!snackbar} autoHideDuration={4000} onClose={() => setSnackbar(null)}>
        {snackbar && (
          <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)}>
            {snackbar.message}
          </Alert>
        )}
      </Snackbar>
    </Box>
  )
}
