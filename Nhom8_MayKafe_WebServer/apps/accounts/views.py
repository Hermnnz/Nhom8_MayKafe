from pathlib import Path

from django.conf import settings
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.views import APIView

from apps.accounts.serializers import LoginSerializer
from apps.common.responses import success_response

LOGIN_AVATAR_RELATIVE_PATH = "branding/MayKafe_Avatar.jpg"


def build_login_branding_payload(request):
    avatar_file = Path(settings.MEDIA_ROOT) / LOGIN_AVATAR_RELATIVE_PATH
    avatar_url = None
    if avatar_file.exists():
        relative_url = f"{settings.MEDIA_URL.rstrip('/')}/{LOGIN_AVATAR_RELATIVE_PATH}"
        avatar_url = request.build_absolute_uri(relative_url)
    return {
        "loginAvatarUrl": avatar_url,
    }


def build_user_payload(user):
    profile = getattr(user, "profile", None)
    role = profile.role if profile else "STAFF"
    display_name = profile.resolved_display_name if profile else (user.get_full_name().strip() or user.username)
    avatar_initials = profile.avatar_initials if profile else display_name[:2].upper()
    avatar_color_hex = profile.avatar_color_hex if profile else "#6B3F2A"
    return {
        "username": user.username,
        "displayName": display_name,
        "role": role,
        "avatarInitials": avatar_initials,
        "avatarColorHex": avatar_color_hex,
    }


class LoginAPIView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        user = serializer.validated_data["user"]
        token, _ = Token.objects.get_or_create(user=user)
        return success_response(
            data={
                "token": token.key,
                "user": build_user_payload(user),
            },
            message="Đăng nhập thành công.",
            status_code=status.HTTP_200_OK,
        )


class BrandingAPIView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        return success_response(
            data=build_login_branding_payload(request),
            message="Lay thong tin avatar dang nhap thanh cong.",
            status_code=status.HTTP_200_OK,
        )


class MeAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return success_response(
            data=build_user_payload(request.user),
            message="Lấy thông tin người dùng thành công.",
        )


class LogoutAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        Token.objects.filter(user=request.user).delete()
        return success_response(message="Đăng xuất thành công.")
