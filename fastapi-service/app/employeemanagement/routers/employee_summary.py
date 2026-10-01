"""
POST /api/ems/ai/employee-summary

Generates a short, HR-facing summary paragraph for one employee, using data
supplied by the Spring Boot proxy (this service has no direct access to the
employees table - Spring Boot/JPA owns that data).
"""
import logging

from fastapi import APIRouter

from app.employeemanagement.schemas import EmployeeSummaryRequest, EmployeeSummaryResponse
from app.employeemanagement.services.llm_client import active_model_name, generate
from app.employeemanagement.services.prompt_utils import describe_employee

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/ems/ai", tags=["AI Assistant"])

SYSTEM_PROMPT = (
    "You are an HR assistant. Write a concise, factual 2-3 sentence summary of the "
    "employee described below, suitable for a manager skimming a team roster. "
    "Only state facts given to you - never invent details such as skills, "
    "performance, or personality traits that were not provided."
)


@router.post("/employee-summary", response_model=EmployeeSummaryResponse)
def summarize_employee(request: EmployeeSummaryRequest):
    logger.info("Generating employee summary for employeeId=%s", request.employee.employeeId)
    summary = generate(SYSTEM_PROMPT, describe_employee(request.employee))
    return EmployeeSummaryResponse(summary=summary, model=active_model_name())
