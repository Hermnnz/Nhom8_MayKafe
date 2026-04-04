from django.conf import settings
from django.db import models


class LegacyAccount(models.Model):
    username = models.CharField(max_length=150, primary_key=True, db_column="ID_TaiKhoan")
    password = models.CharField(max_length=255, db_column="MatKhau")
    role_name = models.CharField(max_length=50, db_column="ChucVu", blank=True, null=True)

    class Meta:
        db_table = "maycafe_taikhoan"
        managed = False
        ordering = ["username"]
        verbose_name = "Legacy account"
        verbose_name_plural = "Legacy accounts"

    def __str__(self) -> str:
        return self.username

    @property
    def normalized_role(self) -> str:
        raw_role = (self.role_name or "").strip().lower()
        if raw_role in {"admin", "quanly", "manager"}:
            return Profile.ROLE_ADMIN
        return Profile.ROLE_STAFF


class Profile(models.Model):
    ROLE_ADMIN = "ADMIN"
    ROLE_STAFF = "STAFF"
    ROLE_CHOICES = (
        (ROLE_ADMIN, "Admin"),
        (ROLE_STAFF, "Staff"),
    )

    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="profile",
    )
    display_name = models.CharField(max_length=120, blank=True)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default=ROLE_STAFF)
    avatar_color_hex = models.CharField(max_length=7, default="#6B3F2A")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["user__username"]

    def __str__(self) -> str:
        return f"{self.user.username} ({self.role})"

    @property
    def resolved_display_name(self) -> str:
        if self.display_name:
            return self.display_name
        full_name = self.user.get_full_name().strip()
        return full_name or self.user.username

    @property
    def avatar_initials(self) -> str:
        tokens = [token for token in self.resolved_display_name.split() if token]
        if len(tokens) >= 2:
            return (tokens[0][0] + tokens[1][0]).upper()
        return self.resolved_display_name[:2].upper()
