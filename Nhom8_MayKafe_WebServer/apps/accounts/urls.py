from django.urls import path

from apps.accounts.views import BrandingAPIView, LoginAPIView, LogoutAPIView, MeAPIView


urlpatterns = [
    path("branding/", BrandingAPIView.as_view(), name="auth-branding"),
    path("login/", LoginAPIView.as_view(), name="auth-login"),
    path("me/", MeAPIView.as_view(), name="auth-me"),
    path("logout/", LogoutAPIView.as_view(), name="auth-logout"),
]
