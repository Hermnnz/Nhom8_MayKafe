from django.contrib import admin
from django.contrib.admin.sites import NotRegistered
from django.contrib.auth import get_user_model
from django.contrib.auth.admin import UserAdmin as DjangoUserAdmin

from apps.accounts.models import Profile


User = get_user_model()


class ProfileInline(admin.StackedInline):
    model = Profile
    can_delete = False
    extra = 0


try:
    admin.site.unregister(User)
except NotRegistered:
    pass


@admin.register(User)
class UserAdmin(DjangoUserAdmin):
    inlines = (ProfileInline,)
    list_display = ("username", "email", "is_staff", "is_active")


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "role", "display_name", "avatar_color_hex")
    list_filter = ("role",)
    search_fields = ("user__username", "display_name")
