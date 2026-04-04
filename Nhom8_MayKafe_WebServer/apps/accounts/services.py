from django.contrib.auth import get_user_model
from django.db import connection
from django.db import transaction
from rest_framework.authtoken.models import Token

from apps.accounts.models import LegacyAccount, Profile


User = get_user_model()


ROLE_COLOR_MAP = {
    Profile.ROLE_ADMIN: "#6B3F2A",
    Profile.ROLE_STAFF: "#C8956C",
}


def ensure_runtime_auth_tables() -> None:
    existing_tables = set(connection.introspection.table_names())

    with connection.schema_editor() as schema_editor:
        if Profile._meta.db_table not in existing_tables:
            schema_editor.create_model(Profile)
            existing_tables.add(Profile._meta.db_table)

        if Token._meta.db_table not in existing_tables:
            schema_editor.create_model(Token)
            existing_tables.add(Token._meta.db_table)


@transaction.atomic
def sync_user_from_legacy_account(account: LegacyAccount, raw_password: str):
    user, _ = User.objects.get_or_create(username=account.username)
    user.is_active = True
    user.is_staff = account.normalized_role == Profile.ROLE_ADMIN
    user.is_superuser = False
    user.set_password(raw_password)
    user.save()

    profile, _ = Profile.objects.get_or_create(user=user)
    profile.display_name = account.username
    profile.role = account.normalized_role
    profile.avatar_color_hex = ROLE_COLOR_MAP.get(profile.role, "#6B3F2A")
    profile.save()

    return user
