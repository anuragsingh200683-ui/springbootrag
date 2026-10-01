import { useEffect, useState } from 'react'
import { Alert, Box, Button, Chip, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { useNavigate } from 'react-router-dom'
import { naturalLanguageSearch } from '../../api/aiApi.js'
import { listDepartments } from '../../api/departmentApi.js'
import { listDesignations } from '../../api/designationApi.js'

export default function NlSearchTab() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [departments, setDepartments] = useState([])
  const [designations, setDesignations] = useState([])

  useEffect(() => {
    listDepartments().then(setDepartments).catch(() => setDepartments([]))
    listDesignations().then(setDesignations).catch(() => setDesignations([]))
  }, [])

  const runSearch = async () => {
    if (!query.trim()) return
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await naturalLanguageSearch(query.trim())
      setResult(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // The AI service only knows department/designation *names* (it has no DB access) -
  // resolve those to ids here since GET /api/ems/employees filters by id.
  const applyToEmployees = () => {
    if (!result) return
    const params = new URLSearchParams()
    if (result.filters.keyword) params.set('keyword', result.filters.keyword)
    if (result.filters.status) params.set('status', result.filters.status)
    if (result.filters.departmentName) {
      const match = departments.find((d) => d.name.toLowerCase() === result.filters.departmentName.toLowerCase())
      if (match) params.set('departmentId', match.id)
    }
    if (result.filters.designationName) {
      const match = designations.find((d) => d.name.toLowerCase() === result.filters.designationName.toLowerCase())
      if (match) params.set('designationId', match.id)
    }
    navigate(`/employees?${params.toString()}`)
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="subtitle1" sx={{ mb: 2 }}>Natural Language Employee Search</Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
        <TextField
          fullWidth size="small"
          placeholder='e.g. "active engineers in the Engineering department named John"'
          value={query} onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') runSearch() }}
        />
        <Button variant="contained" startIcon={<SearchIcon />} disabled={loading || !query.trim()} onClick={runSearch}>
          Search
        </Button>
      </Stack>

      {loading && <CircularProgress size={24} />}
      {error && <Alert severity="error">{error}</Alert>}

      {result && (
        <Box sx={{ mt: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>{result.explanation}</Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
            {result.filters.keyword && <Chip label={`keyword: ${result.filters.keyword}`} />}
            {result.filters.departmentName && <Chip label={`department: ${result.filters.departmentName}`} />}
            {result.filters.designationName && <Chip label={`designation: ${result.filters.designationName}`} />}
            {result.filters.status && <Chip label={`status: ${result.filters.status}`} />}
            {!result.filters.keyword && !result.filters.departmentName && !result.filters.designationName && !result.filters.status && (
              <Chip label="no filters extracted" variant="outlined" />
            )}
          </Stack>
          <Button variant="outlined" onClick={applyToEmployees}>Apply to Employees page</Button>
        </Box>
      )}
    </Paper>
  )
}
