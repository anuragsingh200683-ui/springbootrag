import { Paper, Typography } from '@mui/material'

/** Stand-in for modules not built yet, so routing/nav is fully wired from day one. */
export default function PlaceholderPage({ title }) {
  return (
    <Paper sx={{ p: 4 }}>
      <Typography variant="h5" gutterBottom>{title}</Typography>
      <Typography color="text.secondary">This module is coming soon.</Typography>
    </Paper>
  )
}
