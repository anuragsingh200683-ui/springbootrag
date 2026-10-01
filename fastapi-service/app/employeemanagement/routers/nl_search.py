"""
POST /api/ems/ai/search

Translates a natural-language employee search request into structured filters
(keyword/department/designation/status). This service does NOT execute the
search itself - it has no access to the employees table - it only returns the
filters, which the Spring Boot proxy or the frontend then feeds into the real
GET /api/ems/employees search endpoint.
"""
import json
import logging

from fastapi import APIRouter

from app.employeemanagement.exceptions import AiServiceError
from app.employeemanagement.schemas import NlSearchFilters, NlSearchRequest, NlSearchResponse
from app.employeemanagement.services.llm_client import active_model_name, generate

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/ems/ai", tags=["AI Assistant"])

SYSTEM_PROMPT = (
    "You translate a natural-language employee search request into structured search "
    "filters for an Employee Management System. Respond with ONLY a single JSON object - "
    "no prose, no markdown code fences - matching exactly this shape:\n"
    '{"keyword": string|null, "departmentName": string|null, "designationName": string|null, '
    '"status": "ACTIVE"|"INACTIVE"|null, "explanation": string}\n'
    "keyword should capture a person's name if one is mentioned, else null. departmentName/"
    "designationName should be the department or job title mentioned, in natural casing "
    '(e.g. "Engineering", "Senior Software Engineer"), or null if none is mentioned. status '
    "should be ACTIVE or INACTIVE only if the user clearly asks for active/inactive/former "
    "employees, else null. explanation is one short sentence describing the filters you applied."
)


@router.post("/search", response_model=NlSearchResponse)
def natural_language_search(request: NlSearchRequest):
    logger.info("NL employee search query: %s", request.query)

    raw = generate(SYSTEM_PROMPT, request.query)
    payload = _parse_json_response(raw)

    filters = NlSearchFilters(
        keyword=payload.get("keyword"),
        departmentName=payload.get("departmentName"),
        designationName=payload.get("designationName"),
        status=payload.get("status"),
    )
    explanation = payload.get("explanation") or "Applied the filters shown above."
    return NlSearchResponse(filters=filters, explanation=explanation, model=active_model_name())


def _parse_json_response(raw: str) -> dict:
    text = raw.strip()
    # Defensive: strip markdown code fences if the model added them despite instructions.
    if text.startswith("```"):
        text = text.strip("`")
        if text.lower().startswith("json"):
            text = text[4:]
        text = text.strip()

    try:
        return json.loads(text)
    except (json.JSONDecodeError, TypeError) as exc:
        logger.warning("NL search: could not parse model output as JSON: %r", raw)
        raise AiServiceError(f"Could not parse AI response as JSON: {exc}") from exc
