from rest_framework.test import APITestCase
from django.urls import reverse


class BrandingApiTests(APITestCase):
    def test_branding_endpoint_returns_login_avatar_url(self):
        response = self.client.get(reverse("auth-branding"))

        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.data["success"])
        self.assertEqual(
            response.data["data"]["loginAvatarUrl"],
            "http://testserver/media/branding/MayKafe_Avatar.jpg",
        )
