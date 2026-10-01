import { useEffect, useState } from 'react'
import {
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Stack,
  MenuItem, Grid,
} from '@mui/material'
import { listDepartments } from '../../api/departmentApi.js'
import { listDesignations } from '../../api/designationApi.js'

const EMPTY_FORM = {
  firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', dateOfJoining: '',
  gender: '', address: '', salary: '', status: 'ACTIVE', departmentId: '', designationId: '',
}

export default function EmployeeFormDialog({ open, initialValue, onClose, onSubmit }) {
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [departments, setDepartments] = useState([])
  const [designations, setDesignations] = useState([])

  useEffect(() => {
    if (!open) return
    listDepartments().then(setDepartments).catch(() => setDepartments([]))
    listDesignations().then(setDesignations).catch(() => setDesignations([]))
  }, [open])

  useEffect(() => {
    if (initialValue) {
      setForm({
        firstName: initialValue.firstName || '',
        lastName: initialValue.lastName || '',
        email: initialValue.email || '',
        phone: initialValue.phone || '',
        dateOfBirth: initialValue.dateOfBirth || '',
        dateOfJoining: initialValue.dateOfJoining || '',
        gender: initialValue.gender || '',
        address: initialValue.address || '',
        salary: initialValue.salary ?? '',
        status: initialValue.status || 'ACTIVE',
        departmentId: initialValue.department?.id || '',
        designationId: initialValue.designation?.id || '',
      })
    } else {
      setForm(EMPTY_FORM)
    }
    setErrors({})
  }, [initialValue, open])

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  const validate = () => {
    const next = {}
    if (!form.firstName.trim()) next.firstName = 'First name is required'
    if (!form.lastName.trim()) next.lastName = 'Last name is required'
    if (!form.email.trim()) next.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = 'Enter a valid email'
    if (!form.dateOfJoining) next.dateOfJoining = 'Date of joining is required'
    if (form.salary === '' || Number(form.salary) < 0) next.salary = 'Enter a valid salary'
    if (!form.departmentId) next.departmentId = 'Department is required'
    if (!form.designationId) next.designationId = 'Designation is required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    onSubmit({
      ...form,
      salary: Number(form.salary),
      dateOfBirth: form.dateOfBirth || null,
      gender: form.gender || null,
      departmentId: Number(form.departmentId),
      designationId: Number(form.designationId),
    })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>{initialValue ? 'Edit Employee' : 'Add Employee'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.5 }}>
          <Grid item xs={12} sm={6}>
            <TextField label="First Name" value={form.firstName} onChange={handleChange('firstName')}
              error={!!errors.firstName} helperText={errors.firstName} required fullWidth autoFocus />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Last Name" value={form.lastName} onChange={handleChange('lastName')}
              error={!!errors.lastName} helperText={errors.lastName} required fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Email" value={form.email} onChange={handleChange('email')}
              error={!!errors.email} helperText={errors.email} required fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Phone" value={form.phone} onChange={handleChange('phone')} fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Date of Birth" type="date" value={form.dateOfBirth} onChange={handleChange('dateOfBirth')}
              InputLabelProps={{ shrink: true }} fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Date of Joining" type="date" value={form.dateOfJoining} onChange={handleChange('dateOfJoining')}
              error={!!errors.dateOfJoining} helperText={errors.dateOfJoining}
              InputLabelProps={{ shrink: true }} required fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField select label="Gender" value={form.gender} onChange={handleChange('gender')} fullWidth>
              <MenuItem value="">-</MenuItem>
              <MenuItem value="MALE">Male</MenuItem>
              <MenuItem value="FEMALE">Female</MenuItem>
              <MenuItem value="OTHER">Other</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField label="Salary" type="number" value={form.salary} onChange={handleChange('salary')}
              error={!!errors.salary} helperText={errors.salary} required fullWidth />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField select label="Department" value={form.departmentId} onChange={handleChange('departmentId')}
              error={!!errors.departmentId} helperText={errors.departmentId} required fullWidth>
              {departments.map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField select label="Designation" value={form.designationId} onChange={handleChange('designationId')}
              error={!!errors.designationId} helperText={errors.designationId} required fullWidth>
              {designations.map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField select label="Status" value={form.status} onChange={handleChange('status')} fullWidth>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="INACTIVE">Inactive</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12}>
            <TextField label="Address" value={form.address} onChange={handleChange('address')}
              multiline rows={2} fullWidth />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  )
}
