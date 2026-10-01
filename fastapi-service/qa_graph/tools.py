"""
Tool surface for the Tool Node.

Kept intentionally small (arithmetic + current date/time) to match the
current scope of the app. Add more @tool functions here as needed - graph.py
and nodes.py pick up whatever is exported in TOOLS without further changes
to the graph shape.
"""
import ast
import logging
import operator
from datetime import datetime, timezone

from langchain_core.tools import tool

logger = logging.getLogger(__name__)

_ALLOWED_OPERATORS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.Pow: operator.pow,
    ast.Mod: operator.mod,
    ast.USub: operator.neg,
    ast.UAdd: operator.pos,
}


def _safe_eval(node: ast.AST):
    """
    Evaluates a restricted arithmetic AST - numbers and + - * / ** % only.
    No names, no function calls, no attribute access, so this cannot execute
    arbitrary code the way a raw eval() of user input could.
    """
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return node.value
    if isinstance(node, ast.BinOp) and type(node.op) in _ALLOWED_OPERATORS:
        return _ALLOWED_OPERATORS[type(node.op)](_safe_eval(node.left), _safe_eval(node.right))
    if isinstance(node, ast.UnaryOp) and type(node.op) in _ALLOWED_OPERATORS:
        return _ALLOWED_OPERATORS[type(node.op)](_safe_eval(node.operand))
    raise ValueError("Expression contains unsupported syntax")


@tool
def calculator(expression: str) -> str:
    """Evaluates a basic arithmetic expression, e.g. '12 * (4 + 3)'. Only
    numbers and the + - * / ** % operators are supported."""
    try:
        parsed = ast.parse(expression, mode="eval").body
        return str(_safe_eval(parsed))
    except Exception as exc:
        logger.warning("calculator tool failed for %r: %s", expression, exc)
        return f"Could not evaluate '{expression}': {exc}"


@tool
def current_datetime() -> str:
    """Returns the current UTC date and time."""
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")


TOOLS = [calculator, current_datetime]
TOOLS_BY_NAME = {t.name: t for t in TOOLS}
