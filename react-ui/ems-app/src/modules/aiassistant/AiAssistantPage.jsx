import { useState } from 'react'
import { Box, Tab, Tabs, Typography } from '@mui/material'
import EmployeeInsightsTab from './EmployeeInsightsTab.jsx'
import ChatbotTab from './ChatbotTab.jsx'
import NlSearchTab from './NlSearchTab.jsx'

export default function AiAssistantPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>AI Assistant</Typography>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label="Employee Insights" />
        <Tab label="HR Chatbot" />
        <Tab label="Natural Language Search" />
      </Tabs>

      {tab === 0 && <EmployeeInsightsTab />}
      {tab === 1 && <ChatbotTab />}
      {tab === 2 && <NlSearchTab />}
    </Box>
  )
}
