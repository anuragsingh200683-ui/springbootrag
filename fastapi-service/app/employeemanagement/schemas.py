"""
Pydantic request/response schemas for the EMS AI Assistant module.

This service has no direct database access to employees/departments/designations
(those tables are owned by Spring Boot / JPA) - so employee data is always passed
in the request payload by the Spring Boot proxy (com.example.employeemanagement.aiassistant),
not looked up here.
"""
from typing import List, Optional

from pydantic import BaseModel, Field


class EmployeeProfileInput(BaseModel):
    """Employee facts as known by Spring Boot, forwarded for the LLM to reason over."""
    employeeId: Optional[int] = None
    firstName: str
    lastName: str
    email: Optional[str] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    departmentName: Optional[str] = None
    designationName: Optional[str] = None
    dateOfJoining: Optional[str] = None
    status: Optional[str] = None
    salary: Optional[float] = None


# --- Employee Summary ---

class EmployeeSummaryRequest(BaseModel):
    employee: EmployeeProfileInput


class EmployeeSummaryResponse(BaseModel):
    summary: str
    model: str


# --- Employee Profile Generator ---

class ProfileGenerateRequest(BaseModel):
    employee: EmployeeProfileInput
    tone: Optional[str] = Field(default="professional", description="e.g. professional, friendly, formal")


class ProfileGenerateResponse(BaseModel):
    profile: str
    model: str


# --- HR Chatbot ---

class ChatMessage(BaseModel):
    role: str = Field(..., description="'user' or 'assistant'")
    content: str


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000)
    history: Optional[List[ChatMessage]] = None
    employeeContext: Optional[EmployeeProfileInput] = None


class ChatResponse(BaseModel):
    reply: str
    model: str


# --- Natural Language Employee Search ---

class NlSearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=500)


class NlSearchFilters(BaseModel):
    keyword: Optional[str] = None
    departmentName: Optional[str] = None
    designationName: Optional[str] = None
    status: Optional[str] = Field(default=None, description="ACTIVE or INACTIVE")


class NlSearchResponse(BaseModel):
    filters: NlSearchFilters
    explanation: str
    model: str
