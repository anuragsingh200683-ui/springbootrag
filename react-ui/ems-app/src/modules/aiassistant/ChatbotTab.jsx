import { useState } from 'react'
import { Box, Button, Paper, Stack, TextField, Typography, CircularProgress, Alert, Avatar } from '@mui/material'
import SendIcon from '@mui/icons-material/Send'
import SmartToyIcon from '@mui/icons-material/SmartToy'
import PersonIcon from '@mui/icons-material/Person'
import EmployeeAutocomplete from '../common/EmployeeAutocomplete.jsx'
import { chatWithAssistant } from '../../api/aiApi.js'

export default function ChatbotTab() {
  const [employee, setEmployee] = useState(null)
  const [message, setMessage] = useState('')
  const [history, setHistory] = useState([]) // [{ role, content }]
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const send = async () => {
    const text = message.trim()
    if (!text) return
    const nextHistory = [...history, { role: 'user', content: text }]
    setHistory(nextHistory)
    setMessage('')
    setLoading(true)
    setError('')
    try {
      const res = await chatWithAssistant({
        message: text,
        employeeId: employee?.id,
        history: nextHistory.slice(0, -1), // prior turns only; current message is sent separately
      })
      setHistory((h) => [...h, { role: 'assistant', content: res.reply }])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="subtitle1" sx={{ mb: 2 }}>HR Chatbot</Typography>
      <Box sx={{ maxWidth: 320, mb: 2 }}>
        <EmployeeAutocomplete value={employee} onChange={setEmployee} label="Ground in employee (optional)" />
      </Box>

      <Stack spacing={1.5} sx={{ mb: 2, maxHeight: 360, overflowY: 'auto' }}>
        {history.length === 0 && (
          <Typography color="text.secondary" variant="body2">Ask something like "How many days of leave has this employee taken?"</Typography>
        )}
        {history.map((msg, idx) => (
          <Stack key={idx} direction="row" spacing={1.5} alignItems="flex-start"
            sx={{ flexDirection: msg.role === 'user' ? 'row-reverse' : 'row' }}>
            <Avatar sx={{ bgcolor: msg.role === 'user' ? 'primary.main' : 'secondary.main', width: 32, height: 32 }}>
              {msg.role === 'user' ? <PersonIcon fontSize="small" /> : <SmartToyIcon fontSize="small" />}
            </Avatar>
            <Box sx={{
              p: 1.5, borderRadius: 2, maxWidth: '75%',
              bgcolor: msg.role === 'user' ? 'primary.main' : 'background.default',
              color: msg.role === 'user' ? 'primary.contrastText' : 'text.primary',
            }}>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>{msg.content}</Typography>
            </Box>
          </Stack>
        ))}
        {loading && <CircularProgress size={20} />}
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Stack direction="row" spacing={1}>
        <TextField
          fullWidth size="small" placeholder="Type a message..."
          value={message} onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
        />
        <Button variant="contained" endIcon={<SendIcon />} disabled={loading || !message.trim()} onClick={send}>
          Send
        </Button>
      </Stack>
    </Paper>
  )
}
