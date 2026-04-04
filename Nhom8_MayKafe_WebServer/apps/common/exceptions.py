from django.db import DatabaseError
from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import exception_handler


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)
    if response is None:
        if isinstance(exc, DatabaseError):
            return Response(
                {
                    "success": False,
                    "message": "Database error.",
                    "errors": str(exc),
                },
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )
        return response

    if isinstance(exc, ValidationError):
        response.data = {
            "success": False,
            "message": "Validation error.",
            "errors": response.data,
        }
        return response

    if isinstance(response.data, dict):
        message = response.data.get("detail", "Request failed.")
        errors = response.data if response.status_code != status.HTTP_404_NOT_FOUND else None
    else:
        message = "Request failed."
        errors = response.data

    response.data = {
        "success": False,
        "message": message,
        "errors": errors,
    }
    if response.data["errors"] is None:
        response.data.pop("errors")
    return response
