import os
import socket
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parents[2]


def load_env_file(env_path: Path) -> None:
    if not env_path.exists():
        return

    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip("'").strip('"')
        os.environ.setdefault(key, value)


load_env_file(BASE_DIR / ".env")


def env(key: str, default: str | None = None) -> str | None:
    return os.environ.get(key, default)


def env_bool(key: str, default: bool = False) -> bool:
    value = env(key)
    if value is None:
        return default
    return value.lower() in {"1", "true", "yes", "on"}


def parse_csv(value: str | None) -> list[str]:
    if not value:
        return []
    return [item.strip() for item in value.split(",") if item.strip()]


def get_local_ipv4_hosts() -> list[str]:
    hosts: set[str] = set()
    candidate_names = {socket.gethostname(), socket.getfqdn(), "localhost"}

    for candidate in candidate_names:
        try:
            _, _, addresses = socket.gethostbyname_ex(candidate)
        except OSError:
            continue
        for address in addresses:
            if address and "." in address and not address.startswith("127."):
                hosts.add(address)

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as probe:
            probe.connect(("8.8.8.8", 80))
            address = probe.getsockname()[0]
            if address and "." in address and not address.startswith("127."):
                hosts.add(address)
    except OSError:
        pass

    return sorted(hosts)


def resolve_allowed_hosts() -> list[str]:
    default_hosts = ["127.0.0.1", "localhost", "10.0.2.2"]
    configured_hosts = parse_csv(env("DJANGO_ALLOWED_HOSTS"))
    hosts = configured_hosts or default_hosts.copy()

    if DEBUG:
        for host in default_hosts + get_local_ipv4_hosts():
            if host not in hosts:
                hosts.append(host)

    return hosts


SECRET_KEY = env("DJANGO_SECRET_KEY", "django-insecure-maykafe-local-dev-key")
DEBUG = env_bool("DJANGO_DEBUG", False)

ALLOWED_HOSTS = resolve_allowed_hosts()

INSTALLED_APPS = [
    "corsheaders",
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "rest_framework",
    "rest_framework.authtoken",
    "apps.common",
    "apps.accounts",
    "apps.catalog",
    "apps.sales",
    "apps.reports",
]

MIDDLEWARE = [
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    }
]

WSGI_APPLICATION = "config.wsgi.application"
ASGI_APPLICATION = "config.asgi.application"

DB_ENGINE = env("DB_ENGINE", "sqlite").lower()

if DB_ENGINE == "postgresql":
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.postgresql",
            "NAME": env("DB_NAME", "maykafe"),
            "USER": env("DB_USER", "postgres"),
            "PASSWORD": env("DB_PASSWORD", ""),
            "HOST": env("DB_HOST", "127.0.0.1"),
            "PORT": env("DB_PORT", "5432"),
        }
    }
elif DB_ENGINE == "mysql":
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.mysql",
            "NAME": env("DB_NAME", "maykafe"),
            "USER": env("DB_USER", "root"),
            "PASSWORD": env("DB_PASSWORD", ""),
            "HOST": env("DB_HOST", "127.0.0.1"),
            "PORT": env("DB_PORT", "3306"),
        }
    }
else:
    DATABASES = {
        "default": {
            "ENGINE": "django.db.backends.sqlite3",
            "NAME": BASE_DIR / env("SQLITE_NAME", "db.sqlite3"),
        }
    }

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

LANGUAGE_CODE = "vi"
TIME_ZONE = env("DJANGO_TIME_ZONE", "Asia/Bangkok")
USE_I18N = True
USE_TZ = True

STATIC_URL = "/static/"
STATIC_ROOT = BASE_DIR / "staticfiles"
MEDIA_URL = "/media/"
MEDIA_ROOT = BASE_DIR / "media"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"

REST_FRAMEWORK = {
    "DEFAULT_AUTHENTICATION_CLASSES": [
        "rest_framework.authentication.TokenAuthentication",
        "rest_framework.authentication.SessionAuthentication",
    ],
    "DEFAULT_PERMISSION_CLASSES": [
        "rest_framework.permissions.IsAuthenticated",
    ],
    "DEFAULT_PAGINATION_CLASS": "apps.common.pagination.StandardResultsSetPagination",
    "PAGE_SIZE": int(env("API_PAGE_SIZE", "10")),
    "DEFAULT_RENDERER_CLASSES": [
        "rest_framework.renderers.JSONRenderer",
    ],
    "EXCEPTION_HANDLER": "apps.common.exceptions.custom_exception_handler",
}

CORS_ALLOW_ALL_ORIGINS = env_bool("CORS_ALLOW_ALL_ORIGINS", DEBUG)
CORS_ALLOWED_ORIGINS = parse_csv(env("CORS_ALLOWED_ORIGINS"))

CSRF_TRUSTED_ORIGINS = parse_csv(env("CSRF_TRUSTED_ORIGINS"))
