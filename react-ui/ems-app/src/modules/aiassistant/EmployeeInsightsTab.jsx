import { useState } from 'react'
import { Box, Button, MenuItem, Paper, Stack, TextField, Typography, CircularProgress, Alert } from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import EmployeeAutocomplete from '../common/EmployeeAutocomplete.jsx'
import { getEmployeeAiSummary, generateEmployeeProfile } from '../../api/aiApi.js'

export default function EmployeeInsightsTab() {
  const [employee, setEmployee] = useState(null)
  const [tone, setTone] = useState('professional')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null) // { kind: 'summary'|'profile', text, model }

  const runSummary = async () => {
    if (!employee) return
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await getEmployeeAiSummary(employee.id)
      setResult({ kind: 'summary', text: res.summary, model: res.model })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const runProfile = async () => {
    if (!employee) return
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await generateEmployeeProfile(employee.id, { tone })
      setResult({ kind: 'profile', text: res.profile, model: res.model })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="subtitle1" sx={{ mb: 2 }}>Employee Summary &amp; Profile Generator</Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
        <Box sx={{ minWidth: 300 }}>
          <EmployeeAutocomplete value={employee} onChange={setEmployee} />
        </Box>
        <TextField select label="Tone" size="small" value={tone} onChange={(e) => setTone(e.target.value)} sx={{ minWidth: 160 }}>
          <MenuItem value="professional">Professional</MenuItem>
          <MenuItem value="friendly">Friendly</MenuItem>
          <MenuItem value="formal">Formal</MenuItem>
        </TextField>
        <Button variant="contained" startIcon={<AutoAwesomeIcon />} disabled={!employee || loading} onClick={runSummary}>
          Summary
        </Button>
        <Button variant="outlined" startIcon={<AutoAwesomeIcon />} disabled={!employee || loading} onClick={runProfile}>
          Full Profile
        </Button>
      </Stack>

      {loading && <CircularProgress size={24} />}
      {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      {result && (
        <Box sx={{ mt: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
          <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>{result.text}</Typography>
          <Typography variant="caption" color="text.secondary">Model: {result.model}</Typography>
        </Box>
      )}
    </Paper>
  )
}
