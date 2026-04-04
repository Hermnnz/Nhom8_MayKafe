from rest_framework.permissions import AllowAny
from rest_framework.views import APIView

from apps.common.responses import success_response


class HealthAPIView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        return success_response(message="MayKafe API is running.")
