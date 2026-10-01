"""
Shared helper turning an EmployeeProfileInput into a plain-text block for LLM
prompts. Reused by employee_summary, profile_generator and chatbot routers.
"""
from app.employeemanagement.schemas import EmployeeProfileInput


def describe_employee(employee: EmployeeProfileInput) -> str:
    lines = [f"Name: {employee.firstName} {employee.lastName}"]

    if employee.designationName:
        lines.append(f"Designation: {employee.designationName}")
    if employee.departmentName:
        lines.append(f"Department: {employee.departmentName}")
    if employee.dateOfJoining:
        lines.append(f"Date of joining: {employee.dateOfJoining}")
    if employee.status:
        lines.append(f"Status: {employee.status}")
    if employee.email:
        lines.append(f"Email: {employee.email}")
    if employee.phone:
        lines.append(f"Phone: {employee.phone}")
    if employee.address:
        lines.append(f"Address: {employee.address}")
    if employee.salary is not None:
        lines.append(f"Salary: {employee.salary}")

    return "\n".join(lines)
