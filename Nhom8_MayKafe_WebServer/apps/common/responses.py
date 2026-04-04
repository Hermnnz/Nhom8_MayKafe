from typing import Any

from rest_framework import status
from rest_framework.response import Response


def success_response(
    data: Any = None,
    message: str = "Request completed successfully.",
    status_code: int = status.HTTP_200_OK,
    meta: dict[str, Any] | None = None,
) -> Response:
    payload: dict[str, Any] = {
        "success": True,
        "message": message,
        "data": data,
    }
    if meta is not None:
        payload["meta"] = meta
    return Response(payload, status=status_code)


def error_response(
    message: str = "Request failed.",
    errors: Any = None,
    status_code: int = status.HTTP_400_BAD_REQUEST,
) -> Response:
    payload: dict[str, Any] = {
        "success": False,
        "message": message,
    }
    if errors is not None:
        payload["errors"] = errors
    return Response(payload, status=status_code)
