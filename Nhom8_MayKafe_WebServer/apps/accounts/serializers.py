from django.db import DatabaseError
from rest_framework import serializers

from apps.accounts.models import LegacyAccount
from apps.accounts.services import ensure_runtime_auth_tables, sync_user_from_legacy_account


ACCOUNT_NOT_FOUND_MESSAGE = "T\u00e0i kho\u1ea3n kh\u00f4ng t\u1ed3n t\u1ea1i"
INVALID_CREDENTIALS_MESSAGE = "T\u00e0i kho\u1ea3n ho\u1eb7c m\u1eadt kh\u1ea9u kh\u00f4ng \u0111\u00fang"
RUNTIME_AUTH_TABLES_NOT_READY_MESSAGE = (
    "B\u1ea3ng t\u00e0i kho\u1ea3n ch\u01b0a s\u1eb5n s\u00e0ng \u0111\u1ec3 \u0111\u0103ng nh\u1eadp."
)


class LoginSerializer(serializers.Serializer):
    username = serializers.CharField(max_length=150)
    password = serializers.CharField(max_length=128, trim_whitespace=False)

    def validate(self, attrs):
        username = attrs.get("username", "").strip()
        password = attrs.get("password", "")

        try:
            ensure_runtime_auth_tables()
            account = LegacyAccount.objects.filter(username=username).first()
        except DatabaseError as exc:
            raise serializers.ValidationError(RUNTIME_AUTH_TABLES_NOT_READY_MESSAGE) from exc

        if not account:
            raise serializers.ValidationError(ACCOUNT_NOT_FOUND_MESSAGE)

        if account.password != password:
            raise serializers.ValidationError(INVALID_CREDENTIALS_MESSAGE)

        attrs["user"] = sync_user_from_legacy_account(account, password)
        return attrs


class UserProfileSerializer(serializers.Serializer):
    username = serializers.CharField(read_only=True)
    displayName = serializers.CharField(read_only=True)
    role = serializers.CharField(read_only=True)
    avatarInitials = serializers.CharField(read_only=True)
    avatarColorHex = serializers.CharField(read_only=True)


class LoginResponseSerializer(serializers.Serializer):
    token = serializers.CharField(read_only=True)
    user = UserProfileSerializer(read_only=True)
