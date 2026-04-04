from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.views import APIView

from apps.accounts.serializers import LoginSerializer
from apps.common.responses import success_response


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
