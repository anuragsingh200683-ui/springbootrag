import { Chip } from '@mui/material'

// Covers every status enum used across the EMS modules (employee, attendance, leave).
const COLOR_MAP = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  CHECKED_IN: 'info',
  CHECKED_OUT: 'success',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

export default function StatusChip({ status }) {
  if (!status) return null
  return (
    <Chip
      label={status.replaceAll('_', ' ')}
      color={COLOR_MAP[status] || 'default'}
      size="small"
      variant="outlined"
    />
  )
}
