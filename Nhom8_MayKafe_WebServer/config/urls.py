from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import include, path

from apps.common.views import HealthAPIView

admin.site.site_header = "MayKafe Administration"
admin.site.site_title = "MayKafe Admin"
admin.site.index_title = "Welcome to MayKafe Admin"


urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/v1/auth/", include("apps.accounts.urls")),
    path("api/v1/catalog/", include("apps.catalog.urls")),
    path("api/v1/orders/", include("apps.sales.urls")),
    path("api/v1/reports/", include("apps.reports.urls")),
    path("api/v1/health/", HealthAPIView.as_view(), name="health"),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
