import { useEffect, useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Stack } from '@mui/material'

const EMPTY_FORM = { name: '', description: '' }

export default function DesignationFormDialog({ open, initialValue, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    setForm(initialValue ? { name: initialValue.name, description: initialValue.description || '' } : EMPTY_FORM)
    setErrors({})
  }, [initialValue, open])

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  const validate = () => {
    const next = {}
    if (!form.name.trim()) next.name = 'Name is required'
    else if (form.name.length > 100) next.name = 'Name must be at most 100 characters'
    if (form.description.length > 500) next.description = 'Description must be at most 500 characters'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    onSubmit(form)
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{initialValue ? 'Edit Designation' : 'Add Designation'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Name" value={form.name} onChange={handleChange('name')}
            error={!!errors.name} helperText={errors.name} required fullWidth autoFocus
            placeholder="Senior Software Engineer"
          />
          <TextField
            label="Description" value={form.description} onChange={handleChange('description')}
            error={!!errors.description} helperText={errors.description} multiline rows={3} fullWidth
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  )
}
