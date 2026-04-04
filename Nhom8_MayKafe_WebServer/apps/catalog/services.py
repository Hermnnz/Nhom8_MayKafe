from pathlib import Path
from urllib.parse import urlparse
from uuid import uuid4

from django.conf import settings
from django.core.files.storage import default_storage
from django.utils import timezone


def is_absolute_url(value: str | None) -> bool:
    return bool(value and value.startswith(("http://", "https://")))


def get_media_prefix() -> str:
    return f"/{settings.MEDIA_URL.strip('/')}/"


def normalize_image_reference(value: str | None) -> str | None:
    if value is None:
        return None

    normalized = value.strip()
    if not normalized:
        return None

    media_prefix = get_media_prefix()

    if is_absolute_url(normalized):
        parsed = urlparse(normalized)
        if parsed.path.startswith(media_prefix):
            return parsed.path[len(media_prefix):].lstrip("/")
        return normalized

    if normalized.startswith(media_prefix):
        return normalized[len(media_prefix):].lstrip("/")

    if normalized.startswith(settings.MEDIA_URL):
        return normalized[len(settings.MEDIA_URL) :].lstrip("/")

    if normalized.startswith("/"):
        return normalized

    return normalized


def build_media_url(path: str, request=None) -> str:
    media_url = f"{get_media_prefix()}{path.lstrip('/')}"
    if request is None:
        return media_url
    return request.build_absolute_uri(media_url)


def resolve_image_url(value: str | None, request=None) -> str | None:
    normalized = normalize_image_reference(value)
    if not normalized:
        return None
    if is_absolute_url(normalized):
        return normalized
    if normalized.startswith("/"):
        return request.build_absolute_uri(normalized) if request else normalized
    return build_media_url(normalized, request=request)


def build_asset_label(name: str | None) -> str:
    normalized = (name or "").strip()
    if not normalized:
        return "MK"

    parts = [part for part in normalized.split() if part]
    if not parts:
        return "MK"

    label = "".join(part[0] for part in parts[:2]).upper()
    if len(label) == 1 and len(normalized) > 1:
        label += normalized[1].upper()
    return label or "MK"


def default_accent_color(category_name: str | None) -> str:
    normalized = (category_name or "").strip().lower()
    if "trà" in normalized or "tra" in normalized:
        return "#C8956C"
    if "sinh tố" in normalized or "sinh to" in normalized:
        return "#F5A623"
    if "nước ép" in normalized or "nuoc ep" in normalized:
        return "#F08B3A"
    if "đồ ăn" in normalized or "do an" in normalized or "bánh" in normalized or "banh" in normalized:
        return "#CA9C63"
    return "#6B3F2A"


def store_uploaded_product_image(uploaded_file) -> str:
    extension = Path(uploaded_file.name).suffix.lower() or ".bin"
    filename = f"products/{timezone.now():%Y/%m}/{uuid4().hex}{extension}"
    return default_storage.save(filename, uploaded_file)
