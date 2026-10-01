import { useCallback, useEffect, useState } from 'react'
import {
  Box, Button, IconButton, Paper, Snackbar, Alert, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, CircularProgress,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { listDesignations, createDesignation, updateDesignation, deleteDesignation } from '../../api/designationApi.js'
import DesignationFormDialog from './DesignationFormDialog.jsx'
import ConfirmDialog from '../common/ConfirmDialog.jsx'
import { isAdmin } from '../../auth/keycloak.js'

export default function DesignationPage() {
  const [designations, setDesignations] = useState([])
  const [loading, setLoading] = useState(true)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [snackbar, setSnackbar] = useState(null)
  // Create/edit/delete are ADMIN-only on the API; hide them from everyone else.
  const admin = isAdmin()

  const load = useCallback(() => {
    setLoading(true)
    listDesignations()
      .then(setDesignations)
      .catch((err) => setSnackbar({ severity: 'error', message: err.message }))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const handleAdd = () => { setEditing(null); setFormOpen(true) }
  const handleEdit = (designation) => { setEditing(designation); setFormOpen(true) }

  const handleSubmit = async (form) => {
    try {
      if (editing) {
        await updateDesignation(editing.id, form)
        setSnackbar({ severity: 'success', message: 'Designation updated' })
      } else {
        await createDesignation(form)
        setSnackbar({ severity: 'success', message: 'Designation created' })
      }
      setFormOpen(false)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  const handleDelete = async () => {
    try {
      await deleteDesignation(deleteTarget.id)
      setSnackbar({ severity: 'success', message: 'Designation deleted' })
      setDeleteTarget(null)
      load()
    } catch (err) {
      setSnackbar({ severity: 'error', message: err.message })
    }
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Designations</Typography>
        {admin && <Button variant="contained" startIcon={<AddIcon />} onClick={handleAdd}>Add Designation</Button>}
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
                  <TableCell colSpan={3} align="center"><CircularProgress size={28} sx={{ my: 2 }} /></TableCell>
                </TableRow>
              ) : designations.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} align="center">No designations yet.</TableCell>
                </TableRow>
              ) : (
                designations.map((designation) => (
                  <TableRow key={designation.id} hover>
                    <TableCell>{designation.name}</TableCell>
                    <TableCell>{designation.description}</TableCell>
                    <TableCell align="right">
                      {admin && (
                        <>
                          <IconButton size="small" onClick={() => handleEdit(designation)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton size="small" onClick={() => setDeleteTarget(designation)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <DesignationFormDialog
        open={formOpen}
        initialValue={editing}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete designation"
        message={`Delete "${deleteTarget?.name}"? This cannot be undone.`}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />

      <Snackbar open={!!snackbar} autoHideDuration={4000} onClose={() => setSnackbar(null)}>
        {snackbar && (
          <Alert severity={snackbar.severity} onClose={() => setSnackbar(null)}>{snackbar.message}</Alert>
        )}
      </Snackbar>
    </Box>
  )
}
