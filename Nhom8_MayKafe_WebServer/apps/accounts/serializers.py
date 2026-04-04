from django.db import DatabaseError
from rest_framework import serializers

from apps.accounts.models import LegacyAccount
from apps.accounts.services import ensure_runtime_auth_tables, sync_user_from_legacy_account


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
            raise serializers.ValidationError("Bang tai khoan chua san sang de dang nhap.") from exc

        if not account or account.password != password:
            raise serializers.ValidationError("Ten dang nhap hoac mat khau khong dung.")

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
