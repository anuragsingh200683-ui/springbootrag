import { Paper, Typography, Box } from '@mui/material'

export default function SummaryCard({ label, value, icon, color = 'primary.main' }) {
  return (
    <Paper sx={{ p: 2.5, display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
      <Box sx={{ color, display: 'flex' }}>{icon}</Box>
      <Box>
        <Typography variant="h5">{value}</Typography>
        <Typography variant="body2" color="text.secondary">{label}</Typography>
      </Box>
    </Paper>
  )
}
