"""
POST /api/ems/ai/employee-profile

Generates a longer, narrative "employee directory" style profile (vs. the
shorter roster summary in employee_summary.py), with an adjustable tone.
"""
import logging

from fastapi import APIRouter

from app.employeemanagement.schemas import ProfileGenerateRequest, ProfileGenerateResponse
from app.employeemanagement.services.llm_client import active_model_name, generate
from app.employeemanagement.services.prompt_utils import describe_employee

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/ems/ai", tags=["AI Assistant"])


@router.post("/employee-profile", response_model=ProfileGenerateResponse)
def generate_employee_profile(request: ProfileGenerateRequest):
    tone = request.tone or "professional"
    logger.info("Generating employee profile for employeeId=%s (tone=%s)", request.employee.employeeId, tone)

    system_prompt = (
        f"You are an HR assistant writing a short employee directory profile in a {tone} tone. "
        "Use only the facts given below - never invent skills, achievements, or personal details "
        "that were not provided. Write 3-5 sentences, no headings or bullet points."
    )
    profile = generate(system_prompt, describe_employee(request.employee))
    return ProfileGenerateResponse(profile=profile, model=active_model_name())
