import { useEffect, useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Stack } from '@mui/material'

/** action: 'approve' | 'reject' - controls title/button label/color only. */
export default function LeaveDecisionDialog({ open, action, onClose, onSubmit }) {
  const [decidedBy, setDecidedBy] = useState('')
  const [remarks, setRemarks] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) { setDecidedBy(''); setRemarks(''); setError('') }
  }, [open])

  const isReject = action === 'reject'

  const handleSubmit = () => {
    if (!decidedBy.trim()) { setError('Your name is required'); return }
    onSubmit({ decidedBy, remarks })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{isReject ? 'Reject Leave' : 'Approve Leave'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Decided By" value={decidedBy} onChange={(e) => setDecidedBy(e.target.value)}
            error={!!error} helperText={error} required fullWidth autoFocus />
          <TextField label="Remarks (optional)" value={remarks} onChange={(e) => setRemarks(e.target.value)}
            multiline rows={2} fullWidth />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" color={isReject ? 'error' : 'success'}>
          {isReject ? 'Reject' : 'Approve'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
