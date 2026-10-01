import { useEffect, useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Stack, MenuItem, Box } from '@mui/material'
import EmployeeAutocomplete from '../common/EmployeeAutocomplete.jsx'

const EMPTY_FORM = { leaveType: 'CASUAL', startDate: '', endDate: '', reason: '' }

export default function LeaveApplyDialog({ open, onClose, onSubmit }) {
  const [employee, setEmployee] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    if (open) { setEmployee(null); setForm(EMPTY_FORM); setErrors({}) }
  }, [open])

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  const validate = () => {
    const next = {}
    if (!employee) next.employee = 'Select an employee'
    if (!form.startDate) next.startDate = 'Start date is required'
    if (!form.endDate) next.endDate = 'End date is required'
    else if (form.startDate && form.endDate < form.startDate) next.endDate = 'End date cannot be before start date'
    if (!form.reason.trim()) next.reason = 'Reason is required'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    onSubmit({ ...form, employeeId: employee.id })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Apply for Leave</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <EmployeeAutocomplete value={employee} onChange={setEmployee} />
          {errors.employee && <Box sx={{ color: 'error.main', fontSize: 13, mt: -1 }}>{errors.employee}</Box>}
          <TextField select label="Leave Type" value={form.leaveType} onChange={handleChange('leaveType')} fullWidth>
            <MenuItem value="SICK">Sick</MenuItem>
            <MenuItem value="CASUAL">Casual</MenuItem>
            <MenuItem value="EARNED">Earned</MenuItem>
            <MenuItem value="UNPAID">Unpaid</MenuItem>
          </TextField>
          <Stack direction="row" spacing={2}>
            <TextField label="Start Date" type="date" value={form.startDate} onChange={handleChange('startDate')}
              error={!!errors.startDate} helperText={errors.startDate} InputLabelProps={{ shrink: true }} fullWidth />
            <TextField label="End Date" type="date" value={form.endDate} onChange={handleChange('endDate')}
              error={!!errors.endDate} helperText={errors.endDate} InputLabelProps={{ shrink: true }} fullWidth />
          </Stack>
          <TextField label="Reason" value={form.reason} onChange={handleChange('reason')}
            error={!!errors.reason} helperText={errors.reason} multiline rows={3} fullWidth />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained">Apply</Button>
      </DialogActions>
    </Dialog>
  )
}
