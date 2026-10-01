import { useEffect, useState } from 'react'
import { Autocomplete, TextField } from '@mui/material'
import { listEmployees } from '../../api/employeeApi.js'

/**
 * Reused by Attendance, Leave and AI Assistant - all of them need to pick one
 * employee from the full roster. Loads once (size=200 covers small/medium
 * companies without pagination UI); good enough for this scope, not intended
 * as a scalable server-side search combobox.
 */
export default function EmployeeAutocomplete({ value, onChange, label = 'Employee', ...props }) {
  const [options, setOptions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listEmployees({ page: 0, size: 200, sortBy: 'firstName', sortDir: 'asc' })
      .then((res) => setOptions(res.content))
      .finally(() => setLoading(false))
  }, [])

  return (
    <Autocomplete
      options={options}
      loading={loading}
      value={value || null}
      onChange={(_, newValue) => onChange(newValue)}
      getOptionLabel={(emp) => (emp ? `${emp.firstName} ${emp.lastName} (${emp.employeeCode})` : '')}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      renderInput={(params) => <TextField {...params} label={label} />}
      {...props}
    />
  )
}
