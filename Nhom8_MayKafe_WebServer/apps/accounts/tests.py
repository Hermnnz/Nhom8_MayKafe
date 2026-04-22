from django.db import connection
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase, APITransactionTestCase

from apps.accounts.models import LegacyAccount
from apps.accounts.serializers import ACCOUNT_NOT_FOUND_MESSAGE, INVALID_CREDENTIALS_MESSAGE


class BrandingApiTests(APITestCase):
    def test_branding_endpoint_returns_login_avatar_url(self):
        response = self.client.get(reverse("auth-branding"))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["success"])
        self.assertEqual(
            response.data["data"]["loginAvatarUrl"],
            "http://testserver/media/branding/MayKafe_Avatar.jpg",
        )


class LoginApiTests(APITransactionTestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls._ensure_legacy_account_table()

    @classmethod
    def _ensure_legacy_account_table(cls):
        table_name = connection.ops.quote_name(LegacyAccount._meta.db_table)
        username_column = connection.ops.quote_name("ID_TaiKhoan")
        password_column = connection.ops.quote_name("MatKhau")
        role_column = connection.ops.quote_name("ChucVu")
        with connection.cursor() as cursor:
            cursor.execute(
                f"""
                CREATE TABLE IF NOT EXISTS {table_name} (
                    {username_column} varchar(150) PRIMARY KEY,
                    {password_column} varchar(255) NOT NULL,
                    {role_column} varchar(50) NULL
                )
                """
            )

    def setUp(self):
        super().setUp()
        LegacyAccount.objects.all().delete()
        LegacyAccount.objects.create(
            username="admin",
            password="123456",
            role_name="admin",
        )

    def test_login_returns_account_not_found_when_username_does_not_exist(self):
        response = self.client.post(
            reverse("auth-login"),
            {"username": "ghost", "password": "wrong-pass"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data["success"])
        self.assertEqual(self._extract_validation_message(response), ACCOUNT_NOT_FOUND_MESSAGE)

    def test_login_returns_invalid_credentials_when_password_is_wrong(self):
        response = self.client.post(
            reverse("auth-login"),
            {"username": "admin", "password": "wrong-pass"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(response.data["success"])
        self.assertEqual(self._extract_validation_message(response), INVALID_CREDENTIALS_MESSAGE)

    @staticmethod
    def _extract_validation_message(response):
        errors = response.data.get("errors")
        if isinstance(errors, dict):
            non_field_errors = errors.get("non_field_errors") or []
            if non_field_errors:
                return str(non_field_errors[0])
        if isinstance(errors, list) and errors:
            return str(errors[0])
        return None
